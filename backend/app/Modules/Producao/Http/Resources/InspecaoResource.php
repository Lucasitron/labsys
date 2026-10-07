<?php

namespace App\Modules\Producao\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Inspeção 5S com itens avaliados. */
class InspecaoResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_inspecao,
            'idSetor' => (int) $this->id_setor,
            'setorNome' => $this->setor?->nome,
            'idInspetor' => (int) $this->id_inspetor,
            'dataInspecao' => substr((string) $this->data_inspecao, 0, 10),
            'turno' => $this->turno->value,
            'status' => $this->status->value,
            'observacoes' => $this->observacoes,
            'itens' => ItemInspecaoResource::collection($this->whenLoaded('itens'))->toArray($request),
        ];
    }
}
