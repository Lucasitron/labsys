<?php

namespace App\Modules\Producao\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Sinalização impressa do setor. */
class SinalizacaoResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_sinalizacao,
            'idSetor' => (int) $this->id_setor,
            'texto' => $this->texto,
        ];
    }
}
