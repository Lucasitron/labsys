<?php

namespace App\Modules\Estoque\Events;

use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Events\Dispatchable;

/** Entrada registrada → Financeiro (contas a pagar) + Notification. Nome preservado. */
class CompraSolicitadaEvent implements ShouldQueue
{
    use Dispatchable;

    public const NAME = 'compra.solicitada.event';

    public const QUEUE = 'database';

    public function __construct(
        public readonly int $idEntrada,
        public readonly int $idItem,
        public readonly int $idFornecedor,
        public readonly string $quantidade,
        public readonly string $valorUnitario,
        public readonly string $valorTotal,
        public readonly string $dataEntrada,
        public readonly ?string $notaFiscal,
    ) {}

    /** @return array{idEntrada:int,idItem:int,idFornecedor:int,quantidade:string,valorUnitario:string,valorTotal:string,dataEntrada:string,notaFiscal:?string} */
    public function payload(): array
    {
        return [
            'idEntrada' => $this->idEntrada,
            'idItem' => $this->idItem,
            'idFornecedor' => $this->idFornecedor,
            'quantidade' => $this->quantidade,
            'valorUnitario' => $this->valorUnitario,
            'valorTotal' => $this->valorTotal,
            'dataEntrada' => $this->dataEntrada,
            'notaFiscal' => $this->notaFiscal,
        ];
    }
}
