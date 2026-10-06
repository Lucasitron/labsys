<?php

namespace App\Modules\Rh\Events;

use App\Modules\Rh\Enums\TipoCertificado;
use Carbon\CarbonImmutable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Events\Dispatchable;

/** Certificado aprovado → Notification (avisa funcionário). Nome preservado. */
class CertificadoAprovadoEvent implements ShouldQueue
{
    use Dispatchable;

    public const NAME = 'certificado.aprovado.event';

    public const QUEUE = 'database';

    public function __construct(
        public readonly int $idSolicitacao,
        public readonly int $idCertificado,
        public readonly int $idFuncionario,
        public readonly string $nomeFuncionario,
        public readonly TipoCertificado $tipoCertificado,
        public readonly string $horasCertificadas,
        public readonly CarbonImmutable $dataEmissao,
    ) {}

    /** @return array{idSolicitacao:int,idCertificado:int,idFuncionario:int,nomeFuncionario:string,tipoCertificado:string,horasCertificadas:string,dataEmissao:string} */
    public function payload(): array
    {
        return [
            'idSolicitacao' => $this->idSolicitacao,
            'idCertificado' => $this->idCertificado,
            'idFuncionario' => $this->idFuncionario,
            'nomeFuncionario' => $this->nomeFuncionario,
            'tipoCertificado' => $this->tipoCertificado->value,
            'horasCertificadas' => $this->horasCertificadas,
            'dataEmissao' => $this->dataEmissao->toIso8601String(),
        ];
    }
}
