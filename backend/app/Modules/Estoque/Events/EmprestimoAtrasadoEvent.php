<?php

namespace App\Modules\Estoque\Events;

use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Events\Dispatchable;

/** Empréstimo com devolução vencida → Notification. Nome preservado. */
class EmprestimoAtrasadoEvent implements ShouldQueue
{
    use Dispatchable;

    public const NAME = 'emprestimo.atrasado.event';

    public const QUEUE = 'database';

    public function __construct(
        public readonly int $idEmprestimo,
        public readonly int $idPessoa,
        public readonly int $idItem,
        public readonly string $dataDevolucaoPrevista,
    ) {}

    /** @return array{idEmprestimo:int,idPessoa:int,idItem:int,dataDevolucaoPrevista:string} */
    public function payload(): array
    {
        return [
            'idEmprestimo' => $this->idEmprestimo,
            'idPessoa' => $this->idPessoa,
            'idItem' => $this->idItem,
            'dataDevolucaoPrevista' => $this->dataDevolucaoPrevista,
        ];
    }
}
