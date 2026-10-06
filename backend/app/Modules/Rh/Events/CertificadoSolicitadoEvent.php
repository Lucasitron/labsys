<?php

namespace App\Modules\Rh\Events;

use App\Modules\Rh\Enums\TipoCertificado;
use Carbon\CarbonImmutable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Events\Dispatchable;

/** Certificado solicitado → Notification (avisa Admin). Nome preservado. */
class CertificadoSolicitadoEvent implements ShouldQueue
{
    use Dispatchable;

    public const NAME = 'certificado.solicitado.event';

    public const QUEUE = 'database';

    public function __construct(
        public readonly int $idSolicitacao,
        public readonly int $idFuncionario,
        public readonly TipoCertificado $tipoCertificado,
        public readonly string $horasSolicitadas,
        public readonly CarbonImmutable $dataSolicitacao,
    ) {}

    /** @return array{idSolicitacao:int,idFuncionario:int,tipoCertificado:string,horasSolicitadas:string,dataSolicitacao:string} */
    public function payload(): array
    {
        return [
            'idSolicitacao' => $this->idSolicitacao,
            'idFuncionario' => $this->idFuncionario,
            'tipoCertificado' => $this->tipoCertificado->value,
            'horasSolicitadas' => $this->horasSolicitadas,
            'dataSolicitacao' => $this->dataSolicitacao->toIso8601String(),
        ];
    }
}
