<?php

namespace App\Modules\Producao\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Máquina. */
class MaquinaResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_maquina,
            'nome' => $this->nome,
            'descricao' => $this->descricao,
            'localizacao' => $this->localizacao,
            'status' => $this->status->value,
        ];
    }
}
