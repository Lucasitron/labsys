<?php

namespace App\Modules\Financeiro\Events;

use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Events\Dispatchable;

/** Solicitação de compra registrada → Estoque (`idFornecedor` null no MVP). Nome preservado. */
class CompraSolicitadaEvent implements ShouldQueue
{
    use Dispatchable;

    public const NAME = 'compra.solicitada.event';

    public const QUEUE = 'database';

    public function __construct(
        public readonly int $idCompra,
        public readonly ?int $idFornecedor,
    ) {}

    /** @return array{idCompra:int,idFornecedor:?int} */
    public function payload(): array
    {
        return [
            'idCompra' => $this->idCompra,
            'idFornecedor' => $this->idFornecedor,
        ];
    }
}
