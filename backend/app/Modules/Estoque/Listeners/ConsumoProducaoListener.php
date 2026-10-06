<?php

namespace App\Modules\Estoque\Listeners;

use App\Modules\Estoque\Events\ProducaoConcluidaEvent;
use App\Modules\Estoque\Services\ItemService;
use Illuminate\Contracts\Queue\ShouldQueue;

/**
 * Consome `producao.concluida.event` (fila `database`, sem broker) e efetua a
 * baixa idempotente por `id_referencia` (id da encomenda). Produtor real em M6.
 */
class ConsumoProducaoListener implements ShouldQueue
{
    public const QUEUE = 'database';

    public function __construct(private ItemService $itens) {}

    public function handle(ProducaoConcluidaEvent $event): void
    {
        if ($event->itens === []) {
            return;
        }

        $this->itens->baixarPorConsumo($event->itens, $event->idEncomenda);
    }
}
