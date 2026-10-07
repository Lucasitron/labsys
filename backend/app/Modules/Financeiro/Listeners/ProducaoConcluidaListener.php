<?php

namespace App\Modules\Financeiro\Listeners;

use App\Modules\Estoque\Events\ProducaoConcluidaEvent;
use App\Modules\Financeiro\Services\CusteioService;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Support\Facades\Log;

/**
 * Consome `producao.concluida.event` (fila `database`, sem broker; produtor
 * real em M6/Produção) e dispara o cálculo de custo da encomenda. Evento
 * malformado = warn/ignore; falha nunca propaga (protege o produtor em fila sync).
 */
class ProducaoConcluidaListener implements ShouldQueue
{
    public const QUEUE = 'database';

    public function __construct(private CusteioService $custeio) {}

    public function handle(ProducaoConcluidaEvent $event): void
    {
        try {
            $this->custeio->calcularDoEvento($event->idEncomenda);
        } catch (\Throwable $e) {
            Log::warning("Falha ao processar producao.concluida.event: {$e->getMessage()}");
        }
    }
}
