<?php

namespace App\Modules\Financeiro\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

class FechamentoResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_fechamento,
            'idEncomenda' => (int) $this->id_encomenda,
            'horasEstimadas' => (string) $this->horas_estimadas,
            'valorFechado' => (string) $this->valor_fechado,
            'dataFechamento' => substr((string) $this->data_fechamento, 0, 10),
            'status' => $this->status->value,
            'horasValidadas' => (string) $this->horas_validadas,
        ];
    }
}
