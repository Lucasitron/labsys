<?php

namespace App\Modules\Estoque\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Localização física completa (armário/prateleira/caixa). */
class LocalizacaoResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_localizacao,
            'armario' => $this->armario,
            'prateleira' => $this->prateleira,
            'caixa' => $this->caixa,
            'descricao' => $this->descricao,
        ];
    }
}
