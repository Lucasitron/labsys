<?php

namespace App\Modules\Estoque\Events;

use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Events\Dispatchable;

/**
 * Produção concluída (produtor real em M6/Produção): dispara a baixa
 * automática pelos itens consumidos. Nome preservado.
 */
class ProducaoConcluidaEvent implements ShouldQueue
{
    use Dispatchable;

    public const NAME = 'producao.concluida.event';

    public const QUEUE = 'database';

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
