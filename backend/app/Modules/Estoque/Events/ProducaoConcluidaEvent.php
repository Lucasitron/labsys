<?php

namespace App\Modules\Estoque\Events;

use Illuminate\Foundation\Events\Dispatchable;

/**
 * Produção concluída (produtor real em M6/Produção): dispara a baixa
 * automática pelos itens consumidos. Nome preservado.
 */
class ProducaoConcluidaEvent
{
    use Dispatchable;

    public const NAME = 'producao.concluida.event';

    /**
     * @param list<array{idItem:int,quantidadeConsumida:string|float|int}> $itens
     */
    public function __construct(
        public readonly int $idEncomenda,
        public readonly ?int $idProdutoServico,
        public readonly array $itens,
    ) {}

    /** @return array{idEncomenda:int,idProdutoServico:?int,itens:array} */
    public function payload(): array
    {
        return [
            'idEncomenda' => $this->idEncomenda,
            'idProdutoServico' => $this->idProdutoServico,
            'itens' => $this->itens,
        ];
    }
}
