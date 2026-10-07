<?php

namespace App\Modules\Producao\Services;

use App\Modules\Estoque\Contracts\EstoqueContract;
use App\Modules\Producao\Enums\KanbanStatus;
use App\Modules\Producao\Models\ConsumoEncomenda;
use App\Modules\Producao\Models\EncomendaKanban;
use App\Modules\Producao\Models\HistoricoKanban;
use App\Modules\Producao\Policies\ProducaoPolicy;
use App\Modules\Producao\ProducaoPrincipal;
use App\Modules\Vendas\Contracts\VendasContract;
use App\Shared\Exceptions\ConflitoException;
use App\Shared\Exceptions\ResourceNotFoundException;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\ValidationException;

/**
 * Kanban de produção (F3/F11): LINKA a encomenda do Vendas (404 se
 * inexistente), lock otimista (`version`, 409), histórico + 3 eventos por
 * movimento, baixa idempotente só na 1ª transição a `ENTREGUE` e BOM final
 * via upsert em `consumo_encomenda`.
 */
class KanbanService
{
    public function __construct(
        private VendasContract $vendas,
        private EstoqueContract $estoque,
        private ProducaoEventPublisher $eventos,
    ) {}

    /** @return list<EncomendaKanban> */
    public function listar(?KanbanStatus $status, ProducaoPrincipal $principal): array
    {
        ProducaoPolicy::exigeLeitura($principal);

        return EncomendaKanban::query()
            ->when($status !== null, fn ($q) => $q->where('status', $status))
            ->orderBy('status')->orderBy('ordem')
            ->get()
            ->all();
    }

    public function buscarPorEncomenda(int $idEncomenda, ProducaoPrincipal $principal): EncomendaKanban
    {
        ProducaoPolicy::exigeLeitura($principal);

        return $this->obterPorEncomenda($idEncomenda);
    }

    /** @return list<HistoricoKanban> */
    public function historico(int $idEncomenda, ProducaoPrincipal $principal): array
    {
        ProducaoPolicy::exigeLeitura($principal);

        return HistoricoKanban::where('id_encomenda', $idEncomenda)
            ->orderByDesc('data_alteracao')
            ->get()
            ->all();
    }

    public function incluir(array $dados, ProducaoPrincipal $principal): EncomendaKanban
    {
        ProducaoPolicy::exigeIncluirKanban($principal);

        return DB::transaction(function () use ($dados, $principal) {
            $this->vendas->dadosEncomenda((int) $dados['idEncomenda'])
                ?? throw new ResourceNotFoundException("Encomenda não encontrada: {$dados['idEncomenda']}");

            if (EncomendaKanban::where('id_encomenda', $dados['idEncomenda'])->exists()) {
                throw new ConflitoException("Já existe um cartão no Kanban para a encomenda {$dados['idEncomenda']}");
            }

            $kanban = EncomendaKanban::create([
                'id_encomenda' => $dados['idEncomenda'],
                'id_responsavel' => $dados['idResponsavel'] ?? null,
                'status' => KanbanStatus::FILA->value,
                'data_entrada_status' => now(),
                'ordem' => $dados['ordem'] ?? 0,
                'version' => 0,
            ]);

            $this->registrarHistorico(
                (int) $kanban->id_encomenda, null, KanbanStatus::FILA,
                $principal->idPessoa, 'Cartão incluído',
            );

            return $kanban->refresh();
        });
    }

    public function mover(int $id, KanbanStatus $destino, int $version, ?string $observacao, ProducaoPrincipal $principal): EncomendaKanban
    {
        ProducaoPolicy::exigeLeitura($principal);
        $kanban = $this->obter($id);
        ProducaoPolicy::exigeResponsavel(
            $kanban->id_responsavel === null ? null : (int) $kanban->id_responsavel, $principal,
        );

        return DB::transaction(function () use ($kanban, $destino, $version, $observacao, $principal) {
            $kanban->refresh();

            if ((int) $kanban->version !== $version) {
                throw new ConflitoException('O cartão foi movido por outro usuário. Atualize a tela e tente novamente.');
            }

            $anterior = $kanban->status;
            $kanban->status = $destino;
            $kanban->data_entrada_status = now();
            $kanban->version = $version + 1;
            $kanban->save();

            $this->registrarHistorico(
                (int) $kanban->id_encomenda, $anterior, $destino, $principal->idPessoa, $observacao,
            );

            $this->eventos->statusAlterado(
                (int) $kanban->id_encomenda, $destino, $principal->idPessoa, $observacao,
            );
            $this->eventos->kanbanStatusAlterado((int) $kanban->id_encomenda, $anterior, $destino);

            if ($destino === KanbanStatus::ENTREGUE && $anterior !== KanbanStatus::ENTREGUE) {
                $this->publicarConclusao((int) $kanban->id_encomenda);
            }

            return $kanban->refresh();
        });
    }

    public function remover(int $id, ProducaoPrincipal $principal): void
    {
        ProducaoPolicy::exigeLeitura($principal);
        $kanban = $this->obter($id);
        ProducaoPolicy::exigeResponsavel(
            $kanban->id_responsavel === null ? null : (int) $kanban->id_responsavel, $principal,
        );

        $kanban->delete();
    }

    /**
     * Upsert da BOM final (F11): soma quantidades por `(id_encomenda,id_item)`.
     * A baixa física ocorre só no mover→`ENTREGUE`.
     *
     * @return list<ConsumoEncomenda>
     */
    public function registrarConsumo(int $idEncomenda, array $itens, ProducaoPrincipal $principal): array
    {
        $kanban = EncomendaKanban::where('id_encomenda', $idEncomenda)->first();
        ProducaoPolicy::exigeResponsavel(
            $kanban?->id_responsavel === null ? null : (int) $kanban->id_responsavel, $principal,
        );

        return DB::transaction(function () use ($idEncomenda, $itens) {
            foreach ($itens as $item) {
                if (! $this->estoque->itemExiste((int) $item['idItem'])) {
                    throw ValidationException::withMessages([
                        'itens' => ["Item não encontrado no estoque: {$item['idItem']}"],
                    ]);
                }

                $consumo = ConsumoEncomenda::where('id_encomenda', $idEncomenda)
                    ->where('id_item', $item['idItem'])
                    ->first();

                $quantidade = number_format((float) $item['quantidade'], 2, '.', '');

                if ($consumo === null) {
                    ConsumoEncomenda::create([
                        'id_encomenda' => $idEncomenda,
                        'id_item' => $item['idItem'],
                        'quantidade_consumida' => $quantidade,
                    ]);
                } else {
                    $consumo->quantidade_consumida = number_format(
                        (float) $consumo->quantidade_consumida + (float) $quantidade, 2, '.', '',
                    );
                    $consumo->save();
                }
            }

            return $this->listarConsumoQuery($idEncomenda);
        });
    }

    /** @return list<ConsumoEncomenda> */
    public function listarConsumo(int $idEncomenda, ProducaoPrincipal $principal): array
    {
        ProducaoPolicy::exigeLeitura($principal);

        return $this->listarConsumoQuery($idEncomenda);
    }

    /** Baixa idempotente + evento de conclusão (só na 1ª transição a ENTREGUE). */
    private function publicarConclusao(int $idEncomenda): void
    {
        $itens = [];
        foreach ($this->listarConsumoQuery($idEncomenda) as $consumo) {
            $itens[] = [
                'idItem' => (int) $consumo->id_item,
                'quantidadeConsumida' => (string) $consumo->quantidade_consumida,
            ];
        }

        $this->estoque->baixarConsumo($itens, $idEncomenda);
        $this->eventos->producaoConcluida($idEncomenda, $itens);
    }

    /** @return list<ConsumoEncomenda> */
    private function listarConsumoQuery(int $idEncomenda): array
    {
        return ConsumoEncomenda::where('id_encomenda', $idEncomenda)
            ->orderBy('id_consumo')
            ->get()
            ->all();
    }

    private function registrarHistorico(
        int $idEncomenda,
        ?KanbanStatus $anterior,
        KanbanStatus $novo,
        int $idUsuario,
        ?string $observacao,
    ): void {
        HistoricoKanban::create([
            'id_encomenda' => $idEncomenda,
            'status_anterior' => $anterior?->value,
            'status_novo' => $novo->value,
            'data_alteracao' => now(),
            'id_usuario' => $idUsuario,
            'observacao' => $observacao,
        ]);
    }

    public function obter(int $id): EncomendaKanban
    {
        return EncomendaKanban::find($id)
            ?? throw new ResourceNotFoundException("Cartão do Kanban não encontrado: {$id}");
    }

    private function obterPorEncomenda(int $idEncomenda): EncomendaKanban
    {
        return EncomendaKanban::where('id_encomenda', $idEncomenda)->first()
            ?? throw new ResourceNotFoundException("Cartão do Kanban para a encomenda {$idEncomenda} não encontrado");
    }
}
