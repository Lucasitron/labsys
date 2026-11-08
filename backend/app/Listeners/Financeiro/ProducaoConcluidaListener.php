<?php

namespace App\Listeners\Financeiro;

use App\Events\ProducaoConcluidaEvent;
use App\Services\Financeiro\CusteioService;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Support\Facades\Log;

/**
 * Consome `producao.concluida.event` (fila `database`, sem broker; produtor
 * real em M6/Produção) e dispara o cálculo de custo da encomenda. Evento
 * malformado = warn/ignore; falha nunca propaga (protege o produtor em fila sync).
 */
class ProducaoConcluidaListener implements ShouldQueue
{
    public function __construct(private CusteioService $custeio) {}

    public function handle(ProducaoConcluidaEvent $event): void
    {
        try {
            $this->custeio->registrarMateriais($event->idEncomenda, $event->itens);
            $this->custeio->calcularDoEvento($event->idEncomenda);
        } catch (\Throwable $e) {
            Log::warning("Falha ao processar producao.concluida.event: {$e->getMessage()}");
        }
    }
}
