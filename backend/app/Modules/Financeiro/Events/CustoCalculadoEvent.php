<?php

namespace App\Modules\Financeiro\Events;

use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Events\Dispatchable;

/** Custeio concluído → Vendas & CRM. Nome preservado. */
class CustoCalculadoEvent implements ShouldQueue
{
    use Dispatchable;

    public const NAME = 'custo.calculado.event';

    public const QUEUE = 'database';

    public function __construct(
        public readonly int $idEncomenda,
        public readonly string $custoTotal,
        public readonly string $margemLucro,
        public readonly string $dataCalculo,
    ) {}

    /** @return array{idEncomenda:int,custoTotal:string,margemLucro:string,dataCalculo:string} */
    public function payload(): array
    {
        return [
            'idEncomenda' => $this->idEncomenda,
            'custoTotal' => $this->custoTotal,
            'margemLucro' => $this->margemLucro,
            'dataCalculo' => $this->dataCalculo,
        ];
    }
}
