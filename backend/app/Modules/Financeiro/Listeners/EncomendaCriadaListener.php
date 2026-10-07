<?php

namespace App\Modules\Financeiro\Listeners;

use App\Modules\Financeiro\Services\FechamentoService;
use App\Modules\Vendas\Events\EncomendaCriadaEvent;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Support\Facades\Log;

/**
 * Consome `encomenda.criada.event` (fila `database`, sem broker) e abre o
 * fechamento em ABERTA. Idempotente: fechamento existente = no-op; evento
 * malformado = warn/ignore (nunca 500 — protege o produtor Vendas em fila sync).
 */
class EncomendaCriadaListener implements ShouldQueue
{
    public const QUEUE = 'database';

    public function __construct(private FechamentoService $fechamentos) {}

    public function handle(EncomendaCriadaEvent $event): void
    {
        try {
            $this->fechamentos->criarDoEvento(
                $event->idEncomenda,
                number_format((float) $event->valorFinal, 2, '.', ''),
                substr($event->dataCriacao, 0, 10),
            );
        } catch (\Throwable $e) {
            Log::warning("Falha ao processar encomenda.criada.event: {$e->getMessage()}");
        }
    }
}
