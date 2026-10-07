<?php

namespace App\Modules\Producao\Events;

use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Events\Dispatchable;

/** Cartão mudou de coluna → Vendas e Notification. Nome preservado. */
class KanbanStatusAlteradoEvent implements ShouldQueue
{
    use Dispatchable;

    public const NAME = 'kanban.status.alterado.event';

    public const QUEUE = 'database';

    public function __construct(
        public readonly int $idEncomenda,
        public readonly ?string $statusAnterior,
        public readonly string $statusNovo,
        public readonly string $dataAlteracao,
    ) {}

    /** @return array{idEncomenda:int,statusAnterior:?string,statusNovo:string,dataAlteracao:string} */
    public function payload(): array
    {
        return [
            'idEncomenda' => $this->idEncomenda,
            'statusAnterior' => $this->statusAnterior,
            'statusNovo' => $this->statusNovo,
            'dataAlteracao' => $this->dataAlteracao,
        ];
    }
}
