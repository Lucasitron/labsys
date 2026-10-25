<?php

namespace App\Modules\Vendas\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Interação do CRM. */
class InteracaoResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_interacao,
            'clienteId' => (int) $this->id_cliente,
            'dataInteracao' => (string) $this->data_interacao,
            'tipo' => $this->tipo,
            'descricao' => $this->descricao,
            'idUsuario' => (int) $this->id_usuario,
        ];
    }
}
