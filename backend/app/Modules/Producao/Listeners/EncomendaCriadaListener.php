<?php

namespace App\Modules\Producao\Listeners;

use App\Modules\Producao\Enums\KanbanStatus;
use App\Modules\Producao\Models\EncomendaKanban;
use App\Modules\Vendas\Events\EncomendaCriadaEvent;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Support\Facades\Log;

/**
 * Consome `encomenda.criada.event` do Vendas (fila `database`, sem broker) e
 * insere a encomenda na coluna inicial do Kanban. Idempotente: existente =
 * no-op, nula = warn/ignore.
 */
class EncomendaCriadaListener implements ShouldQueue
{
    public const QUEUE = 'database';

    public function handle(EncomendaCriadaEvent $event): void
    {
        if ($event->idEncomenda === 0) {
            Log::warning('Evento encomenda.criada sem idEncomenda; ignorando');

            return;
        }

        if (EncomendaKanban::where('id_encomenda', $event->idEncomenda)->exists()) {
            return;
        }

        EncomendaKanban::create([
            'id_encomenda' => $event->idEncomenda,
            'status' => KanbanStatus::FILA,
            'data_entrada_status' => now(),
            'ordem' => 0,
            'version' => 0,
        ]);
    }
}
