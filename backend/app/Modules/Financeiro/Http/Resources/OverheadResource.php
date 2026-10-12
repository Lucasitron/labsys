<?php

namespace App\Modules\Financeiro\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

class OverheadResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_parametro,
            'valorTaxaHora' => (string) $this->valor_taxa_hora,
            'dataVigencia' => substr((string) $this->data_vigencia, 0, 10),
        ];
    }
}
