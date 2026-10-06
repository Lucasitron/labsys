<?php

namespace App\Modules\Rh\Events;

use App\Modules\Rh\Enums\TipoCertificado;
use Carbon\CarbonImmutable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Events\Dispatchable;

/** Certificado rejeitado → Notification (avisa funcionário). Nome preservado. */
class CertificadoRejeitadoEvent implements ShouldQueue
{
    use Dispatchable;

    public const NAME = 'certificado.rejeitado.event';

    public const QUEUE = 'database';

    public function __construct(
        public readonly int $idSolicitacao,
        public readonly int $idFuncionario,
        public readonly string $nomeFuncionario,
        public readonly TipoCertificado $tipoCertificado,
        public readonly ?string $observacao,
        public readonly CarbonImmutable $dataDecisao,
    ) {}

    /** @return array{idSolicitacao:int,idFuncionario:int,nomeFuncionario:string,tipoCertificado:string,observacao:?string,dataDecisao:string} */
    public function payload(): array
    {
        return [
            'idSolicitacao' => $this->idSolicitacao,
            'idFuncionario' => $this->idFuncionario,
            'nomeFuncionario' => $this->nomeFuncionario,
            'tipoCertificado' => $this->tipoCertificado->value,
            'observacao' => $this->observacao,
            'dataDecisao' => $this->dataDecisao->toIso8601String(),
        ];
    }
}
