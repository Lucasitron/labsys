<?php

namespace App\Modules\Vendas\Listeners;

use App\Modules\Vendas\Enums\StatusKanban;
use App\Modules\Vendas\Events\ProducaoStatusAlteradoEvent;
use App\Modules\Vendas\Models\Encomenda;
use App\Modules\Vendas\Services\EncomendaService;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Support\Facades\Log;

/**
 * Consome `producao.status.alterado.event` (fila `database`, sem broker) e
 * sincroniza o `status_kanban` da encomenda. Idempotente: status igual =
 * no-op, desconhecido = ignore. `id_usuario=0` (sistema).
 */
class ProducaoStatusListener implements ShouldQueue
{
    public const QUEUE = 'database';

    public function __construct(private EncomendaService $encomendas) {}

    public function handle(ProducaoStatusAlteradoEvent $event): void
    {
        $encomenda = Encomenda::find($event->idEncomenda);

        if ($encomenda === null) {
            Log::warning("Encomenda {$event->idEncomenda} do evento da produção não encontrada");

            return;
        }

        $novo = StatusKanban::tryFrom($event->statusNovo);

        if ($novo === null) {
            Log::warning("Status desconhecido da produção: {$event->statusNovo}");

            return;
        }

        if ($encomenda->status_kanban === $novo) {
            return;
        }

        $anterior = $encomenda->status_kanban->value;
        $encomenda->status_kanban = $novo;
        $encomenda->save();

        $this->encomendas->registrarHistorico(
            (int) $encomenda->getKey(),
            $anterior,
            $novo->value,
            0,
            $event->observacao ?? 'Sincronizado pela produção',
        );
    }
}
