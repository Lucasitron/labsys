<?php

namespace App\Modules\Notification\Listeners;

use App\Modules\Notification\Services\NotificationDispatchService;
use App\Modules\Producao\Events\AdvertenciaLimiteAtingidoEvent;
use App\Modules\Producao\Events\AdvertenciaRegistradaEvent;
use App\Modules\Producao\Events\KanbanStatusAlteradoEvent;
use App\Modules\Producao\Events\ProducaoConcluidaEvent;
use App\Modules\Producao\Events\ProducaoStatusAlteradoEvent;
use App\Modules\Producao\Events\ProjetoMesaAbandonadoEvent;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Support\Facades\Log;

/**
 * Consome os 6 eventos do Produção (fila `database`, sem broker;
 * nomes+payloads preservados — bind pela classe do produtor: `kanban.*` →
 * tipo `producao`). `producao.status` usa `idUsuario ?? admins`. `link` = null.
 */
class ProducaoNotificacaoListener implements ShouldQueue
{
    public const QUEUE = 'database';

    public function __construct(private NotificationDispatchService $dispatch) {}

    public function handleKanbanStatusAlterado(KanbanStatusAlteradoEvent $event): void
    {
        try {
            $this->dispatch->notificarAdmins(
                'producao',
                'Kanban atualizado',
                "Encomenda #{$event->idEncomenda}: {$event->statusAnterior} → {$event->statusNovo}.",
                $event->idEncomenda,
            );
        } catch (\Throwable $e) {
            Log::warning("Falha ao processar kanban.status.alterado.event: {$e->getMessage()}");
        }
    }

    public function handleProducaoStatusAlterado(ProducaoStatusAlteradoEvent $event): void
    {
        try {
            if ($event->idUsuario !== null) {
                $this->dispatch->notificar(
                    'producao',
                    $event->idUsuario,
                    'Produção atualizada',
                    "Encomenda #{$event->idEncomenda} → {$event->statusNovo}."
                    .($event->observacao !== null ? " {$event->observacao}" : ''),
                    $event->idEncomenda,
                    $this->dispatch->emailDe($event->idUsuario),
                );

                return;
            }

            $this->dispatch->notificarAdmins(
                'producao',
                'Produção atualizada',
                "Encomenda #{$event->idEncomenda} → {$event->statusNovo}.",
                $event->idEncomenda,
            );
        } catch (\Throwable $e) {
            Log::warning("Falha ao processar producao.status.alterado.event: {$e->getMessage()}");
        }
    }

    public function handleProducaoConcluida(ProducaoConcluidaEvent $event): void
    {
        try {
            $this->dispatch->notificarAdmins(
                'producao',
                'Produção concluída',
                "Encomenda #{$event->idEncomenda} concluída em {$event->dataConclusao}.",
                $event->idEncomenda,
            );
        } catch (\Throwable $e) {
            Log::warning("Falha ao processar producao.concluida.event: {$e->getMessage()}");
        }
    }

    public function handleAdvertenciaRegistrada(AdvertenciaRegistradaEvent $event): void
    {
        try {
            $this->dispatch->notificarFuncionario(
                'producao',
                $event->idFuncionario,
                'Advertência registrada',
                "Advertência #{$event->contador} registrada: {$event->motivo}",
                $event->idFuncionario,
            );
        } catch (\Throwable $e) {
            Log::warning("Falha ao processar advertencia.registrada.event: {$e->getMessage()}");
        }
    }

    public function handleAdvertenciaLimiteAtingido(AdvertenciaLimiteAtingidoEvent $event): void
    {
        try {
            $this->dispatch->notificarFuncionario(
                'producao',
                $event->idFuncionario,
                'Limite de advertências atingido',
                "Limite atingido ({$event->contador} advertências): {$event->motivo}",
                $event->idFuncionario,
            );
            $this->dispatch->notificarAdmins(
                'producao',
                'Limite de advertências atingido',
                "Funcionário #{$event->idFuncionario} atingiu {$event->contador} advertências: {$event->motivo}",
                $event->idFuncionario,
            );
        } catch (\Throwable $e) {
            Log::warning("Falha ao processar advertencia.limite.atingido.event: {$e->getMessage()}");
        }
    }

    public function handleProjetoMesaAbandonado(ProjetoMesaAbandonadoEvent $event): void
    {
        try {
            $this->dispatch->notificarFuncionario(
                'producao',
                $event->idFuncionario,
                'Projeto de mesa abandonado',
                "Projeto de mesa #{$event->idProjetoMesa} marcado como abandonado."
                .($event->acaoTomada !== null ? " Ação: {$event->acaoTomada}" : ''),
                $event->idProjetoMesa,
            );
            $this->dispatch->notificarAdmins(
                'producao',
                'Projeto de mesa abandonado',
                "Projeto de mesa #{$event->idProjetoMesa} (funcionário #{$event->idFuncionario}) marcado como abandonado.",
                $event->idProjetoMesa,
            );
        } catch (\Throwable $e) {
            Log::warning("Falha ao processar projeto.mesa.abandonado.event: {$e->getMessage()}");
        }
    }
}
