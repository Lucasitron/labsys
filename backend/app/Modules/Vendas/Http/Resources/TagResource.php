<?php

namespace App\Modules\Vendas\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Tag de segmentação. */
class TagResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_tag,
            'nome' => $this->nome,
            'cor' => $this->cor,
        ];
    }
}
