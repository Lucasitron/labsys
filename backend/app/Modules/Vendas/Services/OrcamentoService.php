<?php

namespace App\Modules\Vendas\Services;

use App\Modules\Estoque\Contracts\EstoqueContract;
use App\Modules\Vendas\Enums\StatusOrcamento;
use App\Modules\Vendas\Events\OrcamentoAprovadoEvent;
use App\Modules\Vendas\Models\Cliente;
use App\Modules\Vendas\Models\Encomenda;
use App\Modules\Vendas\Models\ItemOrcamento;
use App\Modules\Vendas\Models\Orcamento;
use App\Modules\Vendas\Policies\VendasPolicy;
use App\Modules\Vendas\VendasPrincipal;
use App\Shared\Exceptions\ConflitoException;
use App\Shared\Exceptions\ResourceNotFoundException;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\ValidationException;

/** Orçamentos com itens, ajustes, aprovação e duplicação (regras só aqui). */
class OrcamentoService
{
    public function criar(array $dados, VendasPrincipal $principal): Orcamento
    {
        VendasPolicy::exigeEscrita($principal);

        Cliente::find($dados['clienteId'])
            ?? throw new ResourceNotFoundException("Cliente não encontrado: {$dados['clienteId']}");

        $this->validarItens($dados['itens']);

        return DB::transaction(function () use ($dados, $principal) {
            $orcamento = Orcamento::create([
                'id_cliente' => $dados['clienteId'],
                'data_criacao' => today()->toDateString(),
                'validade' => $dados['validade'] ?? null,
                'valor_total' => '0.00',
                'status' => StatusOrcamento::PENDENTE,
                'observacoes' => $dados['observacoes'] ?? null,
                'criado_por' => $principal->idPessoa,
            ]);

            $this->salvarItens($orcamento->getKey(), $dados['itens']);
            $orcamento->valor_total = $this->totalDe($orcamento->getKey());
            $orcamento->save();

            return $orcamento->refresh()->load('itens');
        });
    }

    /** @return array{orcamentos:list<Orcamento>,counts:array<string,int>} */
    public function listar(?StatusOrcamento $status, ?int $clienteId, VendasPrincipal $principal): array
    {
        VendasPolicy::exigeLeitura($principal);

        $query = Orcamento::query()->with(['itens', 'cliente'])->withCount('itens')
            ->when($status !== null, fn ($q) => $q->where('status', $status))
            ->when($clienteId !== null, fn ($q) => $q->where('id_cliente', $clienteId))
            ->orderByDesc('id_orcamento');

        $counts = [];
        foreach (StatusOrcamento::cases() as $caso) {
            $counts[$caso->value] = Orcamento::where('status', $caso)->count();
        }

        return ['orcamentos' => $query->get()->all(), 'counts' => $counts];
    }

    public function detalhar(int $id, VendasPrincipal $principal): Orcamento
    {
        VendasPolicy::exigeLeitura($principal);

        return $this->obter($id)->load(['itens', 'cliente']);
    }

    /**
     * Ajustes: edita itens/valores; aprovar publica `orcamento.aprovado.event`.
     * Aprovado+convertido trava edição (409).
     */
    public function atualizar(int $id, array $dados, VendasPrincipal $principal): Orcamento
    {
        VendasPolicy::exigeEscrita($principal);

        return DB::transaction(function () use ($id, $dados) {
            $orcamento = $this->obter($id);

            if ($orcamento->status === StatusOrcamento::APROVADO
                && Encomenda::where('id_orcamento', $id)->exists()) {
                throw new ConflitoException('Orçamento aprovado já convertido em encomenda e não pode ser editado');
            }

            if (($dados['status'] ?? null) !== null && trim((string) $dados['status']) !== '') {
                $novo = $this->paraStatus($dados['status']);
                if ($novo === StatusOrcamento::APROVADO) {
                    $this->aprovar($orcamento);
                } else {
                    $orcamento->status = $novo;
                }
            } elseif (($dados['itens'] ?? null) !== null && $orcamento->status === StatusOrcamento::PENDENTE) {
                $orcamento->status = StatusOrcamento::AJUSTE;
            }

            if (array_key_exists('validade', $dados)) {
                $orcamento->validade = $dados['validade'];
            }
            if (array_key_exists('observacoes', $dados)) {
                $orcamento->observacoes = $dados['observacoes'];
            }

            if (($dados['itens'] ?? null) !== null) {
                $this->validarItens($dados['itens']);
                ItemOrcamento::where('id_orcamento', $id)->delete();
                $this->salvarItens($id, $dados['itens']);
                $orcamento->valor_total = $this->totalDe($id);
            }

            $orcamento->save();

            return $orcamento->refresh()->load(['itens', 'cliente']);
        });
    }

    /** Duplica como Pendente com os mesmos itens (criado_por = atual). */
    public function duplicar(int $id, VendasPrincipal $principal): Orcamento
    {
        VendasPolicy::exigeEscrita($principal);

        return DB::transaction(function () use ($id, $principal) {
            $origem = $this->obter($id);
            $itensOrigem = ItemOrcamento::where('id_orcamento', $id)->get();

            $copia = Orcamento::create([
                'id_cliente' => $origem->id_cliente,
                'data_criacao' => today()->toDateString(),
                'validade' => $origem->validade?->toDateString(),
                'valor_total' => $origem->valor_total,
                'status' => StatusOrcamento::PENDENTE,
                'observacoes' => $origem->observacoes,
                'criado_por' => $principal->idPessoa,
            ]);

            foreach ($itensOrigem as $item) {
                ItemOrcamento::create([
                    'id_orcamento' => $copia->getKey(),
                    'descricao' => $item->descricao,
                    'quantidade' => $item->quantidade,
                    'valor_unitario' => $item->valor_unitario,
                    'material_tipo' => $item->material_tipo,
                    'material_quantidade' => $item->material_quantidade,
                    'material_unidade' => $item->material_unidade,
                    'horas' => $item->horas,
                    'compra' => $item->compra,
                ]);
            }

            return $copia->refresh()->load('itens');
        });
    }

    private function aprovar(Orcamento $orcamento): void
    {
        $orcamento->status = StatusOrcamento::APROVADO;
        $orcamento->save();

        event(new OrcamentoAprovadoEvent(
            (int) $orcamento->getKey(),
            (int) $orcamento->id_cliente,
            number_format((float) $orcamento->valor_total, 2, '.', ''),
        ));
    }

    /** @param list<array<string,mixed>> $itens */
    private function salvarItens(int $idOrcamento, array $itens): void
    {
        foreach ($itens as $dto) {
            ItemOrcamento::create([
                'id_orcamento' => $idOrcamento,
                'descricao' => $dto['descricao'],
                'quantidade' => number_format((float) $dto['quantidade'], 2, '.', ''),
                'valor_unitario' => number_format((float) $dto['valorUnitario'], 2, '.', ''),
                'material_tipo' => $dto['materialTipo'] ?? null,
                'material_quantidade' => isset($dto['materialQuantidade'])
                    ? number_format((float) $dto['materialQuantidade'], 3, '.', '') : null,
                'material_unidade' => $dto['materialUnidade'] ?? null,
                'horas' => isset($dto['horas'])
                    ? number_format((float) $dto['horas'], 2, '.', '') : null,
                'compra' => (bool) ($dto['compra'] ?? false),
            ]);
        }
    }

    /**
     * `idItemEstoque` é transitório (sem coluna): só valida existência (422).
     *
     * @param list<array<string,mixed>> $itens
     */
    private function validarItens(array $itens): void
    {
        $estoque = app(EstoqueContract::class);

        foreach ($itens as $i => $dto) {
            $idItem = $dto['idItemEstoque'] ?? null;
            if ($idItem !== null && ! $estoque->itemExiste((int) $idItem)) {
                throw ValidationException::withMessages([
                    "itens.{$i}.idItemEstoque" => ["Item de estoque inexistente: {$idItem}"],
                ]);
            }
        }
    }

    private function totalDe(int $idOrcamento): string
    {
        $total = 0.0;
        foreach (ItemOrcamento::where('id_orcamento', $idOrcamento)->get() as $item) {
            $total += (float) $item->quantidade * (float) $item->valor_unitario;
        }

        return number_format($total, 2, '.', '');
    }

    private function paraStatus(string $status): StatusOrcamento
    {
        return StatusOrcamento::tryFrom(trim($status))
            ?? throw new \InvalidArgumentException('Status de orçamento inválido (Pendente, Aprovado, Recusado ou Ajuste)');
    }

    private function obter(int $id): Orcamento
    {
        return Orcamento::find($id)
            ?? throw new ResourceNotFoundException("Orçamento não encontrado: {$id}");
    }
}
