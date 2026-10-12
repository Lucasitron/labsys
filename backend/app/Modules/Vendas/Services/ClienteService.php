<?php

namespace App\Modules\Vendas\Services;

use App\Modules\Vendas\Enums\TipoPessoa;
use App\Modules\Vendas\Models\Cliente;
use App\Modules\Vendas\Models\Encomenda;
use App\Modules\Vendas\Models\InteracaoCliente;
use App\Modules\Vendas\Models\Orcamento;
use App\Modules\Vendas\Models\TagCliente;
use App\Modules\Vendas\Policies\VendasPolicy;
use App\Modules\Vendas\Rules\DocumentoRule;
use App\Modules\Vendas\VendasPrincipal;
use App\Shared\Exceptions\ConflitoException;
use App\Shared\Exceptions\ResourceNotFoundException;
use Illuminate\Contracts\Pagination\LengthAwarePaginator;

/** Gestão de clientes PF/PJ (regras só aqui). Escrita = Admin (matriz prevalece). */
class ClienteService
{
    public function criar(array $dados, VendasPrincipal $principal): Cliente
    {
        VendasPolicy::exigeEscrita($principal);

        $digitos = $this->documentoUnico($dados['cpfCnpj']);

        return Cliente::create([
            'tipo_pessoa' => TipoPessoa::from($dados['tipoPessoa']),
            'nome_razao_social' => $dados['nomeRazaoSocial'],
            'cpf_cnpj' => $digitos,
            'email' => $dados['email'] ?? null,
            'telefone' => $dados['telefone'] ?? null,
            'endereco' => $dados['endereco'] ?? null,
            'data_cadastro' => today()->toDateString(),
            'criado_por' => $principal->idPessoa,
        ]);
    }

    public function listar(
        ?string $termo,
        ?TipoPessoa $tipo,
        array $tagIds,
        int $page,
        int $pageSize,
        VendasPrincipal $principal,
    ): LengthAwarePaginator {
        VendasPolicy::exigeLeitura($principal);

        $termo = trim((string) $termo);

        return Cliente::query()
            ->with('tags')
            ->when($termo !== '', fn ($q) => $q->where(
                fn ($w) => $w->where('nome_razao_social', 'ilike', "%{$termo}%")
                    ->orWhere('email', 'ilike', "%{$termo}%"),
            ))
            ->when($tipo !== null, fn ($q) => $q->where('tipo_pessoa', $tipo))
            ->when($tagIds !== [], fn ($q) => $q->whereHas(
                'tags',
                fn ($t) => $t->whereIn('vendas.tag_cliente.id_tag', $tagIds),
            ))
            ->orderBy('nome_razao_social')
            ->paginate(min(max($pageSize, 1), 100), ['*'], 'page', max($page, 1));
    }

    /** @return array{cliente:Cliente,indicadores:array<string,mixed>} */
    public function detalhar(int $id, VendasPrincipal $principal): array
    {
        VendasPolicy::exigeLeitura($principal);

        $cliente = $this->obter($id);
        $cliente->load('tags');

        return [
            'cliente' => $cliente,
            'indicadores' => [
                'orcamentosEmAberto' => Orcamento::where('id_cliente', $id)
                    ->whereIn('status', ['Pendente', 'Ajuste'])->count(),
                'encomendasEmProducao' => Encomenda::where('id_cliente', $id)
                    ->where('status_kanban', '!=', 'Entregue')->count(),
                'ultimaInteracao' => ($ultima = InteracaoCliente::where('id_cliente', $id)
                    ->max('data_interacao')) === null ? null : (string) $ultima,
            ],
        ];
    }

    public function atualizar(int $id, array $dados, VendasPrincipal $principal): Cliente
    {
        VendasPolicy::exigeEscrita($principal);

        $cliente = $this->obter($id);
        $digitos = $this->documentoUnico($dados['cpfCnpj'], $id);

        $cliente->fill([
            'tipo_pessoa' => TipoPessoa::from($dados['tipoPessoa']),
            'nome_razao_social' => $dados['nomeRazaoSocial'],
            'cpf_cnpj' => $digitos,
            'email' => $dados['email'] ?? null,
            'telefone' => $dados['telefone'] ?? null,
            'endereco' => $dados['endereco'] ?? null,
        ]);
        $cliente->save();

        return $cliente->refresh()->load('tags');
    }

    public function excluir(int $id, VendasPrincipal $principal): void
    {
        VendasPolicy::exigeEscrita($principal);

        $cliente = $this->obter($id);

        if (Encomenda::where('id_cliente', $id)->exists()) {
            throw new ConflitoException('Cliente possui encomendas vinculadas e não pode ser removido');
        }

        $cliente->tags()->detach();
        $cliente->delete();
    }

    public function vincularTag(int $idCliente, int $idTag, VendasPrincipal $principal): void
    {
        VendasPolicy::exigeEscrita($principal);

        $cliente = $this->obter($idCliente);
        TagCliente::find($idTag) ?? throw new ResourceNotFoundException("Tag não encontrada: {$idTag}");

        $cliente->tags()->syncWithoutDetaching([$idTag]);
    }

    public function desvincularTag(int $idCliente, int $idTag, VendasPrincipal $principal): void
    {
        VendasPolicy::exigeEscrita($principal);

        $this->obter($idCliente)->tags()->detach($idTag);
    }

    /** Marca tag em lote, sem duplicidade (Admin-only). @return array{vinculados:int,tagId:int} */
    public function bulkTag(array $clienteIds, int $tagId, VendasPrincipal $principal): array
    {
        VendasPolicy::exigeEscrita($principal);

        if ($clienteIds === []) {
            throw new \InvalidArgumentException('Informe ao menos um cliente');
        }

        TagCliente::find($tagId) ?? throw new ResourceNotFoundException("Tag não encontrada: {$tagId}");

        $vinculados = 0;
        foreach ($clienteIds as $idCliente) {
            $cliente = $this->obter((int) $idCliente);
            if ($cliente->tags()->where('vendas.tag_cliente.id_tag', $tagId)->doesntExist()) {
                $cliente->tags()->attach($tagId);
                $vinculados++;
            }
        }

        return ['vinculados' => $vinculados, 'tagId' => $tagId];
    }

    /**
     * Valida o documento (dígitos) e a unicidade. Devolve só-dígitos p/ persistir.
     *
     * @throws \InvalidArgumentException|ConflitoException
     */
    private function documentoUnico(string $documento, ?int $excetoId = null): string
    {
        if (! DocumentoRule::valido($documento)) {
            throw new \InvalidArgumentException('CPF ou CNPJ inválido');
        }

        $digitos = DocumentoRule::normalizar($documento);

        $existe = Cliente::where('cpf_cnpj', $digitos)
            ->when($excetoId !== null, fn ($q) => $q->where('id_cliente', '!=', $excetoId))
            ->exists();

        if ($existe) {
            throw new ConflitoException('CPF ou CNPJ já cadastrado');
        }

        return $digitos;
    }

    private function obter(int $id): Cliente
    {
        return Cliente::find($id)
            ?? throw new ResourceNotFoundException("Cliente não encontrado: {$id}");
    }
}
