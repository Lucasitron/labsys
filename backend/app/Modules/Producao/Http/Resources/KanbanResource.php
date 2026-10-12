<?php

namespace App\Modules\Producao\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Cartão do Kanban (com `version` p/ lock otimista). */
class KanbanResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_kanban,
            'idEncomenda' => (int) $this->id_encomenda,
            'status' => $this->status->value,
            'dataEntradaStatus' => (string) $this->data_entrada_status,
            'idResponsavel' => $this->id_responsavel === null ? null : (int) $this->id_responsavel,
            'ordem' => (int) $this->ordem,
            'version' => (int) $this->version,
        ];
    }
}
