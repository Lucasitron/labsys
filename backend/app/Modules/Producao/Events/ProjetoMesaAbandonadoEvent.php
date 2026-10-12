<?php

namespace App\Modules\Producao\Events;

use Illuminate\Foundation\Events\Dispatchable;

/** Mesa abandonada → RH e Notification. Nome preservado. */
class ProjetoMesaAbandonadoEvent
{
    use Dispatchable;

    public const NAME = 'projeto.mesa.abandonado.event';

    public function __construct(
        public readonly int $idProjetoMesa,
        public readonly int $idFuncionario,
        public readonly ?string $acaoTomada,
    ) {}

    /** @return array{idProjetoMesa:int,idFuncionario:int,acaoTomada:?string} */
    public function payload(): array
    {
        return [
            'idProjetoMesa' => $this->idProjetoMesa,
            'idFuncionario' => $this->idFuncionario,
            'acaoTomada' => $this->acaoTomada,
        ];
    }
}
