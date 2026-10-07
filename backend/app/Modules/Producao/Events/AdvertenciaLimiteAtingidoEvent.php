<?php

namespace App\Modules\Producao\Events;

use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Events\Dispatchable;

/** Limite de advertências atingido (≥3) → Notification/Admin. Nome preservado. */
class AdvertenciaLimiteAtingidoEvent implements ShouldQueue
{
    use Dispatchable;

    public const NAME = 'advertencia.limite.atingido.event';

    public const QUEUE = 'database';

    public function __construct(
        public readonly int $idFuncionario,
        public readonly int $contador,
        public readonly string $motivo,
    ) {}

    /** @return array{idFuncionario:int,contador:int,motivo:string} */
    public function payload(): array
    {
        return [
            'idFuncionario' => $this->idFuncionario,
            'contador' => $this->contador,
            'motivo' => $this->motivo,
        ];
    }
}
