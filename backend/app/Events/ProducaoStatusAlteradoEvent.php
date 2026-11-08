<?php

namespace App\Events;

use Illuminate\Foundation\Events\Dispatchable;

class ProducaoStatusAlteradoEvent
{
    use Dispatchable;

    public const NAME = 'producao.status.alterado.event';

    public readonly string $statusVendas;

    public function __construct(
        public readonly int $idEncomenda,
        public readonly string $statusNovo,
        public readonly ?int $idUsuario = null,
        public readonly ?string $observacao = null,
        ?string $statusVendas = null,
    ) {
        $this->statusVendas = $statusVendas ?? $statusNovo;
    }

    /** @return array{idEncomenda:int,statusNovo:string,statusVendas:string,idUsuario:?int,observacao:?string} */
    public function payload(): array
    {
        return [
            'idEncomenda' => $this->idEncomenda,
            'statusNovo' => $this->statusNovo,
            'statusVendas' => $this->statusVendas,
            'idUsuario' => $this->idUsuario,
            'observacao' => $this->observacao,
        ];
    }
}
