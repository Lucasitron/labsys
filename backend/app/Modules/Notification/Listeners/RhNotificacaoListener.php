<?php

namespace App\Modules\Notification\Listeners;

use App\Modules\Notification\Services\NotificationDispatchService;
use App\Modules\Rh\Events\CertificadoAprovadoEvent;
use App\Modules\Rh\Events\CertificadoRejeitadoEvent;
use App\Modules\Rh\Events\CertificadoSolicitadoEvent;
use App\Modules\Rh\Events\ExtratoMensalHorasEvent;
use App\Modules\Rh\Events\HorasValidadasEvent;
use App\Modules\Rh\Events\NivelAlteradoEvent;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Support\Facades\Log;

/**
 * Consome os 6 eventos do RH (fila `database`, sem broker; nomes+payloads
 * preservados). Malformado = warn/ignore (nunca 500 — protege o produtor em
 * fila sync). `link` = null (sem inventar paths do front — D12).
 */
class RhNotificacaoListener implements ShouldQueue
{
    public const QUEUE = 'database';

    public function __construct(private NotificationDispatchService $dispatch) {}

    public function handleNivelAlterado(NivelAlteradoEvent $event): void
    {
        $this->executar('nivel.alterado', function () use ($event): void {
            $this->dispatch->notificarFuncionario(
                'pessoas',
                $event->idFuncionario,
                'Nível de acesso alterado',
                "Seu nível de acesso foi alterado de {$event->nivelAntigo->label()} para {$event->nivelNovo->label()}.",
                $event->idFuncionario,
            );
        });
    }

    public function handleHorasValidadas(HorasValidadasEvent $event): void
    {
        $this->executar('horas.validadas', function () use ($event): void {
            $this->dispatch->notificarFuncionario(
                'pessoas',
                $event->idFuncionario,
                'Horas validadas',
                "Foram validadas {$event->horas}h ({$event->tipo->value}) em {$event->data}.",
                $event->idReferencia,
            );
        });
    }

    public function handleExtratoMensal(ExtratoMensalHorasEvent $event): void
    {
        $this->executar('extrato.mensal.horas', function () use ($event): void {
            $this->dispatch->notificarFuncionario(
                'pessoas',
                $event->idFuncionario,
                'Extrato mensal de horas disponível',
                "Referência {$event->mesReferencia}: presença {$event->horasPresenca}h, disponíveis {$event->horasDisponiveis}h.",
            );
        });
    }

    public function handleCertificadoSolicitado(CertificadoSolicitadoEvent $event): void
    {
        $this->executar('certificado.solicitado', function () use ($event): void {
            $this->dispatch->notificarAdmins(
                'pessoas',
                'Nova solicitação de certificado',
                "O funcionário #{$event->idFuncionario} solicitou certificado {$event->tipoCertificado->value} ({$event->horasSolicitadas}h).",
                $event->idSolicitacao,
            );
        });
    }

    public function handleCertificadoAprovado(CertificadoAprovadoEvent $event): void
    {
        $this->executar('certificado.aprovado', function () use ($event): void {
            $this->dispatch->notificarFuncionario(
                'pessoas',
                $event->idFuncionario,
                'Certificado aprovado',
                "Sua solicitação #{$event->idSolicitacao} foi aprovada ({$event->horasCertificadas}h certificadas).",
                $event->idSolicitacao,
            );
        });
    }

    public function handleCertificadoRejeitado(CertificadoRejeitadoEvent $event): void
    {
        $this->executar('certificado.rejeitado', function () use ($event): void {
            $this->dispatch->notificarFuncionario(
                'pessoas',
                $event->idFuncionario,
                'Certificado rejeitado',
                "Sua solicitação #{$event->idSolicitacao} foi rejeitada."
                .($event->observacao !== null ? " Motivo: {$event->observacao}" : ''),
                $event->idSolicitacao,
            );
        });
    }

    private function executar(string $evento, \Closure $acao): void
    {
        try {
            $acao();
        } catch (\Throwable $e) {
            Log::warning("Falha ao processar {$evento}.event: {$e->getMessage()}");
        }
    }
}
