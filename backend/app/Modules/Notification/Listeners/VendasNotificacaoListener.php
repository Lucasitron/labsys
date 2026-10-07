<?php

namespace App\Modules\Notification\Listeners;

use App\Modules\Notification\Services\NotificationDispatchService;
use App\Modules\Vendas\Events\EncomendaCriadaEvent;
use App\Modules\Vendas\Events\EncomendaStatusAlteradoEvent;
use App\Modules\Vendas\Events\OrcamentoAprovadoEvent;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Support\Facades\Log;

/**
 * Consome os 3 eventos do Vendas (fila `database`, sem broker;
 * nomes+payloads preservados; `orcamento.aprovado` → tipo `encomenda`).
 * `link` = null (D12).
 */
class VendasNotificacaoListener implements ShouldQueue
{
    public const QUEUE = 'database';

    public function __construct(private NotificationDispatchService $dispatch) {}

    public function handleEncomendaCriada(EncomendaCriadaEvent $event): void
    {
        try {
            $this->dispatch->notificarAdmins(
                'encomenda',
                'Nova encomenda',
                "Encomenda #{$event->idEncomenda} criada p/ o cliente #{$event->idCliente} (valor {$event->valorFinal}).",
                $event->idEncomenda,
            );
        } catch (\Throwable $e) {
            Log::warning("Falha ao processar encomenda.criada.event: {$e->getMessage()}");
        }
    }

    public function handleEncomendaStatusAlterado(EncomendaStatusAlteradoEvent $event): void
    {
        try {
            $this->dispatch->notificarAdmins(
                'encomenda',
                'Encomenda mudou de status',
                "Encomenda #{$event->idEncomenda}: {$event->statusAnterior} → {$event->statusNovo}.",
                $event->idEncomenda,
            );
        } catch (\Throwable $e) {
            Log::warning("Falha ao processar encomenda.status.alterado.event: {$e->getMessage()}");
        }
    }

    public function handleOrcamentoAprovado(OrcamentoAprovadoEvent $event): void
    {
        try {
            $this->dispatch->notificarAdmins(
                'encomenda',
                'Orçamento aprovado',
                "Orçamento #{$event->idOrcamento} aprovado p/ o cliente #{$event->idCliente} (total {$event->valorTotal}).",
                $event->idOrcamento,
            );
        } catch (\Throwable $e) {
            Log::warning("Falha ao processar orcamento.aprovado.event: {$e->getMessage()}");
        }
    }
}
