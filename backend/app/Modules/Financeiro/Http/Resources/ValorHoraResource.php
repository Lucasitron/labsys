<?php

namespace App\Modules\Financeiro\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

class ValorHoraResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_valor_hora,
            'nivelAcesso' => (int) $this->nivel_acesso,
            'valorHora' => (string) $this->valor_hora,
            'dataVigencia' => substr((string) $this->data_vigencia, 0, 10),
        ];
    }
}
