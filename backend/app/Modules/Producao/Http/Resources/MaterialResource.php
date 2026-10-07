<?php

namespace App\Modules\Producao\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Material esperado do setor. */
class MaterialResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_material,
            'idSetor' => (int) $this->id_setor,
            'descricao' => $this->descricao,
            'quantidade' => (string) $this->quantidade,
        ];
    }
}
