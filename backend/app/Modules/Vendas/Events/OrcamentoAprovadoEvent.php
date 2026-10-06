<?php

namespace App\Modules\Vendas\Events;

use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Events\Dispatchable;

/** Orçamento aprovado → Financeiro. Nome preservado. */
class OrcamentoAprovadoEvent implements ShouldQueue
{
    use Dispatchable;

    public const NAME = 'orcamento.aprovado.event';

    public const QUEUE = 'database';

    public function __construct(
        public readonly int $idOrcamento,
        public readonly int $idCliente,
        public readonly string $valorTotal,
    ) {}

    /** @return array{idOrcamento:int,idCliente:int,valorTotal:string} */
    public function payload(): array
    {
        return [
            'idOrcamento' => $this->idOrcamento,
            'idCliente' => $this->idCliente,
            'valorTotal' => $this->valorTotal,
        ];
    }
}
