<?php

namespace App\Modules\Vendas\Events;

use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Events\Dispatchable;

/**
 * Status vindo do Produção (fila `database`, sem broker). Produtor real em
 * M6 — até lá, testado com dispatch manual.
 */
class ProducaoStatusAlteradoEvent implements ShouldQueue
{
    use Dispatchable;

    public const NAME = 'producao.status.alterado.event';

    public const QUEUE = 'database';

    public function __construct(
        public readonly int $idEncomenda,
        public readonly string $statusNovo,
        public readonly ?string $observacao = null,
    ) {}

    /** @return array{idEncomenda:int,statusNovo:string,observacao:?string} */
    public function payload(): array
    {
        return [
            'idEncomenda' => $this->idEncomenda,
            'statusNovo' => $this->statusNovo,
            'observacao' => $this->observacao,
        ];
    }
}
