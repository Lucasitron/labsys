<?php

namespace App\Modules\Financeiro\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

class CategoriaResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_categoria,
            'nome' => $this->nome,
            'tipo' => $this->tipo->value,
            'descricao' => $this->descricao,
        ];
    }
}
