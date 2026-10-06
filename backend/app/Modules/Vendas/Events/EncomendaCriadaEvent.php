<?php

namespace App\Modules\Vendas\Events;

use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Events\Dispatchable;

/** Encomenda criada → Produção. Nome preservado. */
class EncomendaCriadaEvent implements ShouldQueue
{
    use Dispatchable;

    public const NAME = 'encomenda.criada.event';

    public const QUEUE = 'database';

    public function __construct(
        public readonly int $idEncomenda,
        public readonly int $idCliente,
        public readonly string $valorFinal,
        public readonly string $dataCriacao,
    ) {}

    /** @return array{idEncomenda:int,idCliente:int,valorFinal:string,dataCriacao:string} */
    public function payload(): array
    {
        return [
            'idEncomenda' => $this->idEncomenda,
            'idCliente' => $this->idCliente,
            'valorFinal' => $this->valorFinal,
            'dataCriacao' => $this->dataCriacao,
        ];
    }
}
