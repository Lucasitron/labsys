<?php

namespace App\Modules\Estoque\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Saída de estoque. */
class SaidaResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_saida,
            'idItem' => (int) $this->id_item,
            'nomeItem' => $this->item?->nome,
            'quantidade' => (string) $this->quantidade,
            'tipoSaida' => $this->tipo_saida->value,
            'idReferencia' => $this->id_referencia === null ? null : (int) $this->id_referencia,
            'dataSaida' => (string) $this->data_saida,
            'observacao' => $this->observacao,
            'responsavel' => $this->responsavel,
        ];
    }
}
