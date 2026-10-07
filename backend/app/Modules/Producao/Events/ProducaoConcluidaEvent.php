<?php

namespace App\Modules\Producao\Events;

use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Events\Dispatchable;

/** Produção concluída → Estoque (baixa) e Financeiro (custeio). Nome preservado. */
class ProducaoConcluidaEvent implements ShouldQueue
{
    use Dispatchable;

    public const NAME = 'producao.concluida.event';

    public const QUEUE = 'database';

    /**
     * @param list<array{idItem:int,quantidadeConsumida:string}> $itens BOM final
     */
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
