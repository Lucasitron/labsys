<?php

namespace App\Modules\Producao\Events;

use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Events\Dispatchable;

/** Mudança de coluna do Kanban → Vendas (sync) e Notification. Nome preservado. */
class ProducaoStatusAlteradoEvent implements ShouldQueue
{
    use Dispatchable;

    public const NAME = 'producao.status.alterado.event';

    public const QUEUE = 'database';

    public function __construct(
        public readonly int $idEncomenda,
        public readonly string $statusNovo,
        public readonly ?int $idUsuario,
        public readonly ?string $observacao = null,
    ) {}

    /** @return array{idEncomenda:int,statusNovo:string,idUsuario:?int,observacao:?string} */
    public function payload(): array
    {
        return [
            'idEncomenda' => $this->idEncomenda,
            'statusNovo' => $this->statusNovo,
            'idUsuario' => $this->idUsuario,
            'observacao' => $this->observacao,
        ];
    }
}
