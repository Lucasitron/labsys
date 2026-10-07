<?php

namespace App\Modules\Financeiro\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

class DoacaoResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_doacao,
            'tipo' => $this->tipo->value,
            'origem' => $this->origem,
            'valor' => (string) $this->valor,
            'dataRecebimento' => substr((string) $this->data_recebimento, 0, 10),
            'idProjetoAssociado' => $this->id_projeto_associado === null
                ? null : (int) $this->id_projeto_associado,
        ];
    }
}
