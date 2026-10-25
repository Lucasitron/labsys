<?php

namespace App\Modules\Producao\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Registro de movimentação do Kanban. */
class HistoricoKanbanResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_historico,
            'idEncomenda' => (int) $this->id_encomenda,
            'statusAnterior' => $this->status_anterior === null ? null : $this->status_anterior->value,
            'statusNovo' => $this->status_novo->value,
            'dataAlteracao' => (string) $this->data_alteracao,
            'idUsuario' => $this->id_usuario === null ? null : (int) $this->id_usuario,
            'observacao' => $this->observacao,
        ];
    }
}
