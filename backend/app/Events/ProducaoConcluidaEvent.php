<?php

namespace App\Events;

use Illuminate\Foundation\Events\Dispatchable;

class ProducaoConcluidaEvent
{
    use Dispatchable;

    public const NAME = 'producao.concluida.event';

    /** @param list<array{idItem:int,quantidadeConsumida:string}> $itens */
    public function __construct(
        public readonly int $idEncomenda,
        public readonly ?int $idProdutoServico,
        public readonly array $itens,
        public readonly string $dataConclusao,
    ) {}

    /** @return array{idEncomenda:int,idProdutoServico:?int,itens:array,dataConclusao:string} */
    public function payload(): array
    {
        return [
            'idEncomenda' => $this->idEncomenda,
            'idProdutoServico' => $this->idProdutoServico,
            'itens' => $this->itens,
            'dataConclusao' => $this->dataConclusao,
        ];
    }
}