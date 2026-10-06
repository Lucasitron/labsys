<?php

namespace App\Modules\Estoque\Events;

use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Events\Dispatchable;

/** Item igual/abaixo do mínimo → Notification. Nome preservado. */
class EstoqueBaixoEvent implements ShouldQueue
{
    use Dispatchable;

    public const NAME = 'estoque.baixo.event';

    public const QUEUE = 'database';

    public function __construct(
        public readonly int $idItem,
        public readonly string $nome,
        public readonly string $quantidadeAtual,
        public readonly string $estoqueMinimo,
    ) {}

    /** @return array{idItem:int,nome:string,quantidadeAtual:string,estoqueMinimo:string} */
    public function payload(): array
    {
        return [
            'idItem' => $this->idItem,
            'nome' => $this->nome,
            'quantidadeAtual' => $this->quantidadeAtual,
            'estoqueMinimo' => $this->estoqueMinimo,
        ];
    }
}
