<?php

namespace App\Modules\Notification\Listeners;

use App\Modules\Estoque\Events\CompraSolicitadaEvent;
use App\Modules\Estoque\Events\EmprestimoAtrasadoEvent;
use App\Modules\Estoque\Events\EstoqueBaixoEvent;
use App\Modules\Notification\Services\NotificationDispatchService;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Support\Facades\Log;

/**
 * Consome os 3 eventos do Estoque (fila `database`, sem broker;
 * nomes+payloads preservados). `emprestimo.atrasado` usa `idPessoa` direto
 * (= `id_usuario`). `link` = null (D12).
 */
class EstoqueNotificacaoListener implements ShouldQueue
{
    public const QUEUE = 'database';

    public function __construct(private NotificationDispatchService $dispatch) {}

    public function handleEstoqueBaixo(EstoqueBaixoEvent $event): void
    {
        try {
            $this->dispatch->notificarAdmins(
                'estoque',
                'Estoque baixo',
                "O item {$event->nome} (#{$event->idItem}) está com {$event->quantidadeAtual} (mínimo {$event->estoqueMinimo}).",
                $event->idItem,
            );
        } catch (\Throwable $e) {
            Log::warning("Falha ao processar estoque.baixo.event: {$e->getMessage()}");
        }
    }

    public function handleEmprestimoAtrasado(EmprestimoAtrasadoEvent $event): void
    {
        try {
            $this->dispatch->notificar(
                'estoque',
                $event->idPessoa,
                'Empréstimo atrasado',
                "O empréstimo #{$event->idEmprestimo} está com devolução vencida (prevista p/ {$event->dataDevolucaoPrevista}).",
                $event->idEmprestimo,
                $this->dispatch->emailDe($event->idPessoa),
            );
        } catch (\Throwable $e) {
            Log::warning("Falha ao processar emprestimo.atrasado.event: {$e->getMessage()}");
        }
    }

    public function handleCompraSolicitada(CompraSolicitadaEvent $event): void
    {
        try {
            $this->dispatch->notificarAdmins(
                'estoque',
                'Compra solicitada',
                "Entrada #{$event->idEntrada}: item #{$event->idItem}, {$event->quantidade} × {$event->valorUnitario} (total {$event->valorTotal}).",
                $event->idEntrada,
            );
        } catch (\Throwable $e) {
            Log::warning("Falha ao processar compra.solicitada.event: {$e->getMessage()}");
        }
    }
}
