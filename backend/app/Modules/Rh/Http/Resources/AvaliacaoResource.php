<?php

namespace App\Modules\Rh\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

class AvaliacaoResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id,
            'idTreinamento' => (int) $this->id_treinamento,
            'idFuncionario' => (int) $this->id_funcionario,
            'nota' => number_format((float) $this->nota, 2, '.', ''),
            'feedback' => $this->feedback,
            'dataAvaliacao' => $this->data_avaliacao !== null ? substr((string) $this->data_avaliacao, 0, 10) : null,
        ];
    }
}
