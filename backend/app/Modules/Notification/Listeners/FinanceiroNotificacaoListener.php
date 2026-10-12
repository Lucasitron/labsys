<?php

namespace App\Modules\Notification\Listeners;

use App\Modules\Financeiro\Events\CompraSolicitadaEvent;
use App\Modules\Financeiro\Events\LancamentoVencidoEvent;
use App\Modules\Notification\Services\NotificationDispatchService;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Support\Facades\Log;

/**
 * Consome o evento do Financeiro (fila `database`, sem broker; nome+payload
 * preservados). Só Admins (módulo Financeiro é exclusivo Admin — RBAC).
 * Sem listener p/ `custo.calculado` (fora da lista do despacho). `link` = null.
 */
class FinanceiroNotificacaoListener implements ShouldQueue
{
    public const QUEUE = 'database';

    public function __construct(private NotificationDispatchService $dispatch) {}

    public function handleLancamentoVencido(LancamentoVencidoEvent $event): void
    {
        try {
            $this->dispatch->notificarAdmins(
                'financeiro',
                'Lançamento vencido',
                "Lançamento #{$event->idLancamento} venceu em {$event->dataVencimento} (valor {$event->valor}).",
                $event->idLancamento,
            );
        } catch (\Throwable $e) {
            Log::warning("Falha ao processar lancamento.vencido.event: {$e->getMessage()}");
        }
    }

    /** Mesmo NAME do Estoque (`compra.solicitada.event`), outra classe — tipo por origem. */
    public function handleCompraSolicitada(CompraSolicitadaEvent $event): void
    {
        try {
            $this->dispatch->notificarAdmins(
                'financeiro',
                'Compra solicitada',
                "Solicitação de compra #{$event->idCompra} registrada.",
                $event->idCompra,
            );
        } catch (\Throwable $e) {
            Log::warning("Falha ao processar compra.solicitada.event: {$e->getMessage()}");
        }
    }
}
