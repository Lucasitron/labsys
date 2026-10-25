<?php

namespace App\Modules\Producao\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Parâmetro 5S. */
class ParametroResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_parametro,
            'chave' => $this->chave,
            'valor' => $this->valor,
            'descricao' => $this->descricao,
        ];
    }
}
