<?php

namespace App\Modules\Vendas\Services;

use App\Modules\Vendas\Enums\StatusKanban;
use App\Modules\Vendas\Enums\StatusOrcamento;
use App\Modules\Vendas\Events\EncomendaCriadaEvent;
use App\Modules\Vendas\Events\EncomendaStatusAlteradoEvent;
use App\Modules\Vendas\Models\Cliente;
use App\Modules\Vendas\Models\Encomenda;
use App\Modules\Vendas\Models\HistoricoStatusEncomenda;
use App\Modules\Vendas\Models\ItemOrcamento;
use App\Modules\Vendas\Models\Orcamento;
use App\Modules\Vendas\Policies\VendasPolicy;
use App\Modules\Vendas\VendasPrincipal;
use App\Shared\Exceptions\ConflitoException;
use App\Shared\Exceptions\ResourceNotFoundException;
use Illuminate\Support\Facades\DB;

/**
 * Ciclo de vida da encomenda: conversão 1:1 de orçamento aprovado (ou venda
 * direta), Kanban com histórico + lock otimista (`versao`, 409) e nova ordem
 * por alteração. Criador+Admin no Kanban (D-3).
 */
class EncomendaService
{
    public function criar(array $dados, VendasPrincipal $principal): Encomenda
    {
        VendasPolicy::exigeEscrita($principal);

        return DB::transaction(function () use ($dados, $principal) {
            $encomenda = new Encomenda([
                'data_criacao' => today()->toDateString(),
                'data_previsao_entrega' => $dados['dataPrevisaoEntrega'] ?? null,
                'status_kanban' => StatusKanban::FILA,
                'observacoes' => $dados['observacoes'] ?? null,
                'versao' => 0,
                'criado_por' => $principal->idPessoa,
            ]);

            if (($dados['idOrcamento'] ?? null) !== null) {
                $orcamento = Orcamento::find($dados['idOrcamento'])
                    ?? throw new ResourceNotFoundException("Orçamento não encontrado: {$dados['idOrcamento']}");

                if ($orcamento->status !== StatusOrcamento::APROVADO) {
                    throw new ConflitoException('Encomenda só pode nascer de orçamento aprovado');
                }
                if (Encomenda::where('id_orcamento', $orcamento->getKey())->exists()) {
                    throw new ConflitoException('Orçamento já convertido em encomenda');
                }

                $encomenda->id_orcamento = $orcamento->getKey();
                $encomenda->id_cliente = $orcamento->id_cliente;
                $encomenda->valor_final = number_format((float) ($dados['valorFinal'] ?? $orcamento->valor_total), 2, '.', '');
            } else {
                if (($dados['clienteId'] ?? null) === null) {
                    throw new \InvalidArgumentException('Informe o orçamento aprovado ou o cliente (venda direta)');
                }
                Cliente::find($dados['clienteId'])
                    ?? throw new ResourceNotFoundException("Cliente não encontrado: {$dados['clienteId']}");
                if (($dados['valorFinal'] ?? null) === null) {
                    throw new \InvalidArgumentException('O valor da venda direta é obrigatório');
                }

                $encomenda->id_cliente = $dados['clienteId'];
                $encomenda->valor_final = number_format((float) $dados['valorFinal'], 2, '.', '');
            }

            $encomenda->save();

            $this->registrarHistorico(
                (int) $encomenda->getKey(), null, StatusKanban::FILA->value,
                $principal->idPessoa, 'Encomenda criada na Fila',
            );

            event(new EncomendaCriadaEvent(
                (int) $encomenda->getKey(),
                (int) $encomenda->id_cliente,
                number_format((float) $encomenda->valor_final, 2, '.', ''),
                (string) $encomenda->data_criacao->toDateString(),
            ));

            return $encomenda->refresh()->load(['cliente', 'historico']);
        });
    }

    /** @return array{encomendas:list<Encomenda>,counts:array<string,int>} */
    public function listar(?StatusKanban $status, ?int $clienteId, VendasPrincipal $principal): array
    {
        VendasPolicy::exigeLeitura($principal);

        $query = Encomenda::query()->with('cliente')
            ->when($status !== null, fn ($q) => $q->where('status_kanban', $status))
            ->when($clienteId !== null, fn ($q) => $q->where('id_cliente', $clienteId))
            ->orderByDesc('id_encomenda');

        $counts = [];
        foreach (StatusKanban::cases() as $caso) {
            $counts[$caso->value] = Encomenda::where('status_kanban', $caso)->count();
        }

        return ['encomendas' => $query->get()->all(), 'counts' => $counts];
    }

    public function detalhar(int $id, VendasPrincipal $principal): Encomenda
    {
        VendasPolicy::exigeLeitura($principal);

        return $this->obter($id)->load(['cliente', 'historico']);
    }

    /** @return list<ItemOrcamento> itens herdados do orçamento (vazio em venda direta) */
    public function itensDe(int $id, VendasPrincipal $principal): array
    {
        $encomenda = $this->detalhar($id, $principal);

        if ($encomenda->id_orcamento === null) {
            return [];
        }

        return ItemOrcamento::where('id_orcamento', $encomenda->id_orcamento)->get()->all();
    }

    /**
     * Move o cartão (só criador ou Admin; `versao` divergente → 409;
     * `Entregue` trava).
     */
    public function moverKanban(
        int $id,
        StatusKanban $destino,
        int $versao,
        ?string $observacao,
        VendasPrincipal $principal,
    ): Encomenda {
        VendasPolicy::exigirCriadorOuAdmin(
            ($atual = $this->obter($id))->criado_por === null ? null : (int) $atual->criado_por,
            $principal,
        );

        return DB::transaction(function () use ($id, $destino, $versao, $observacao, $principal) {
            $encomenda = $this->obter($id);

            if ((int) $encomenda->versao !== $versao) {
                throw new ConflitoException('O cartão foi movido por outro usuário. Atualize a tela e tente novamente.');
            }
            if ($encomenda->status_kanban === StatusKanban::ENTREGUE) {
                throw new ConflitoException('Encomenda entregue não pode ser movida');
            }

            $anterior = $encomenda->status_kanban;
            $encomenda->status_kanban = $destino;
            $encomenda->versao = $versao + 1;
            $encomenda->save();

            $this->registrarHistorico($id, $anterior->value, $destino->value, $principal->idPessoa, $observacao);

            event(new EncomendaStatusAlteradoEvent(
                $id, $anterior->value, $destino->value, now()->toDateTimeString(),
            ));

            return $encomenda->refresh()->load(['cliente', 'historico']);
        });
    }

    /**
     * Alteração: encerra a atual (histórico) + cria nova ordem referenciando
     * a origem.
     */
    public function novaOrdem(int $id, array $dados, VendasPrincipal $principal): Encomenda
    {
        $atual = $this->obter($id);
        VendasPolicy::exigirCriadorOuAdmin(
            $atual->criado_por === null ? null : (int) $atual->criado_por,
            $principal,
        );

        return DB::transaction(function () use ($atual, $dados, $principal) {
            $this->registrarHistorico(
                (int) $atual->getKey(), $atual->status_kanban->value, $atual->status_kanban->value,
                $principal->idPessoa, 'Encerrada por alteração — nova ordem criada',
            );

            $nova = Encomenda::create([
                'id_cliente' => $atual->id_cliente,
                'data_criacao' => today()->toDateString(),
                'data_previsao_entrega' => $dados['dataPrevisaoEntrega'] ?? null,
                'status_kanban' => StatusKanban::FILA,
                'valor_final' => number_format((float) $dados['valorFinal'], 2, '.', ''),
                'observacoes' => $dados['observacoes'] ?? null,
                'versao' => 0,
                'criado_por' => $principal->idPessoa,
                'encomenda_origem_id' => $atual->getKey(),
            ]);

            $this->registrarHistorico(
                (int) $nova->getKey(), null, StatusKanban::FILA->value,
                $principal->idPessoa, 'Nova ordem a partir da encomenda '.$atual->getKey(),
            );

            event(new EncomendaCriadaEvent(
                (int) $nova->getKey(),
                (int) $nova->id_cliente,
                number_format((float) $nova->valor_final, 2, '.', ''),
                (string) $nova->data_criacao->toDateString(),
            ));

            return $nova->refresh()->load(['cliente', 'historico']);
        });
    }

    /** Leitura do histórico (HistoricoService dobrado aqui, sem classe extra). */
    public function historico(int $idEncomenda, VendasPrincipal $principal): array
    {
        VendasPolicy::exigeLeitura($principal);

        $this->obter($idEncomenda);

        return HistoricoStatusEncomenda::where('id_encomenda', $idEncomenda)
            ->orderBy('data_alteracao')
            ->get()
            ->all();
    }

    public function registrarHistorico(
        int $idEncomenda,
        ?string $anterior,
        string $novo,
        int $idUsuario,
        ?string $observacao,
    ): void {
        HistoricoStatusEncomenda::create([
            'id_encomenda' => $idEncomenda,
            'status_anterior' => $anterior ?? '—',
            'status_novo' => $novo,
            'data_alteracao' => now()->toDateTimeString(),
            'id_usuario' => $idUsuario,
            'observacao' => $observacao,
        ]);
    }

    private function obter(int $id): Encomenda
    {
        return Encomenda::find($id)
            ?? throw new ResourceNotFoundException("Encomenda não encontrada: {$id}");
    }
}
