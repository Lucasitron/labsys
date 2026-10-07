<?php

namespace App\Modules\Financeiro\Events;

use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Events\Dispatchable;

/** Lançamento vencido sem liquidação → Notification. Nome preservado. */
class LancamentoVencidoEvent implements ShouldQueue
{
    use Dispatchable;

    public const NAME = 'lancamento.vencido.event';

    public const QUEUE = 'database';

    public function __construct(
        public readonly int $idLancamento,
        public readonly string $valor,
        public readonly string $dataVencimento,
        public readonly ?string $idReferenciaExterna,
    ) {}

    /** @return array{idLancamento:int,valor:string,dataVencimento:string,idReferenciaExterna:?string} */
    public function payload(): array
    {
        return [
            'idLancamento' => $this->idLancamento,
            'valor' => $this->valor,
            'dataVencimento' => $this->dataVencimento,
            'idReferenciaExterna' => $this->idReferenciaExterna,
        ];
    }
}
