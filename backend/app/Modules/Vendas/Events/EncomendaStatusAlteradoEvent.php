<?php

namespace App\Modules\Vendas\Events;

use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Events\Dispatchable;

/** Kanban movido → Produção + Notification. Nome preservado. */
class EncomendaStatusAlteradoEvent implements ShouldQueue
{
    use Dispatchable;

    public const NAME = 'encomenda.status.alterado.event';

    public const QUEUE = 'database';

    public function __construct(
        public readonly int $idEncomenda,
        public readonly string $statusAnterior,
        public readonly string $statusNovo,
        public readonly string $dataAlteracao,
    ) {}

    /** @return array{idEncomenda:int,statusAnterior:string,statusNovo:string,dataAlteracao:string} */
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
