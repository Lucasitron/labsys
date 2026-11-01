<?php

namespace App\Events;

use Illuminate\Foundation\Events\Dispatchable;

class CompraSolicitadaEvent
{
    use Dispatchable;

    public const NAME = 'compra.solicitada.event';

    public function __construct(
        public readonly ?int $idEntrada = null,
        public readonly ?int $idItem = null,
        public readonly ?int $idFornecedor = null,
        public readonly ?string $quantidade = null,
        public readonly ?string $valorUnitario = null,
        public readonly ?string $valorTotal = null,
        public readonly ?string $dataEntrada = null,
        public readonly ?string $notaFiscal = null,
        public readonly ?int $idCompra = null,
    ) {}

    /** @return array{idEntrada:?int,idItem:?int,idFornecedor:?int,quantidade:?string,valorUnitario:?string,valorTotal:?string,dataEntrada:?string,notaFiscal:?string,idCompra:?int} */
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
            'idCompra' => $this->idCompra,
        ];
    }
}