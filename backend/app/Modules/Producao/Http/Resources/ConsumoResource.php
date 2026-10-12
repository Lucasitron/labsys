<?php

namespace App\Modules\Producao\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Item da BOM final por encomenda. */
class ConsumoResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_consumo,
            'idEncomenda' => (int) $this->id_encomenda,
            'idItem' => (int) $this->id_item,
            'quantidadeConsumida' => (string) $this->quantidade_consumida,
        ];
    }
}
