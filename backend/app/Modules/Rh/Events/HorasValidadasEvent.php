<?php

namespace App\Modules\Rh\Events;

use App\Modules\Rh\Enums\TipoApontamento;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Events\Dispatchable;

/** Horas validadas → Financeiro (custo/coerência). Nome preservado. */
class HorasValidadasEvent implements ShouldQueue
{
    use Dispatchable;

    public const NAME = 'horas.validadas.event';

    public const QUEUE = 'database';

    public function __construct(
        public readonly int $idFuncionario,
        public readonly TipoApontamento $tipo,
        public readonly int $idReferencia,
        public readonly string $horas,
        public readonly string $data,
    ) {}

    /** @return array{idFuncionario:int,tipo:string,idReferencia:int,horas:string,data:string} */
    public function payload(): array
    {
        return [
            'idFuncionario' => $this->idFuncionario,
            'tipo' => $this->tipo->value,
            'idReferencia' => $this->idReferencia,
            'horas' => $this->horas,
            'data' => $this->data,
        ];
    }
}
