<?php

namespace App\Modules\Rh\Events;

use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Events\Dispatchable;

/** Extrato mensal → Notification (e-mail). 1 evento por ativo não-Recrutando. */
class ExtratoMensalHorasEvent implements ShouldQueue
{
    use Dispatchable;

    public const NAME = 'extrato.mensal.horas.event';

    public const QUEUE = 'database';

    public function __construct(
        public readonly int $idFuncionario,
        public readonly string $nome,
        public readonly string $mesReferencia,
        public readonly string $horasPresenca,
        public readonly string $horasEncomenda,
        public readonly string $horasProjeto,
        public readonly string $horasDisponiveis,
    ) {}

    /** @return array{idFuncionario:int,nome:string,mesReferencia:string,horasPresenca:string,horasEncomenda:string,horasProjeto:string,horasDisponiveis:string} */
    public function payload(): array
    {
        return [
            'idFuncionario' => $this->idFuncionario,
            'nome' => $this->nome,
            'mesReferencia' => $this->mesReferencia,
            'horasPresenca' => $this->horasPresenca,
            'horasEncomenda' => $this->horasEncomenda,
            'horasProjeto' => $this->horasProjeto,
            'horasDisponiveis' => $this->horasDisponiveis,
        ];
    }
}
