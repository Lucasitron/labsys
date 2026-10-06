<?php

namespace App\Modules\Rh\Events;

use App\Modules\Rh\Enums\NivelAcesso;
use Carbon\CarbonImmutable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Events\Dispatchable;

/** Nível alterado → Auth (claims) + Notification. Nome preservado. */
class NivelAlteradoEvent implements ShouldQueue
{
    use Dispatchable;

    public const NAME = 'nivel.alterado.event';

    public const QUEUE = 'database';

    public function __construct(
        public readonly int $idFuncionario,
        public readonly NivelAcesso $nivelAntigo,
        public readonly NivelAcesso $nivelNovo,
        public readonly CarbonImmutable $data,
    ) {}

    /** @return array{idFuncionario:int,nivelAntigo:int,nivelNovo:int,data:string} */
    public function payload(): array
    {
        return [
            'idFuncionario' => $this->idFuncionario,
            'nivelAntigo' => $this->nivelAntigo->value,
            'nivelNovo' => $this->nivelNovo->value,
            'data' => $this->data->toIso8601String(),
        ];
    }
}
