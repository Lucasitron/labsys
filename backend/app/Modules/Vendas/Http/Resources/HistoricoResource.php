<?php

namespace App\Modules\Vendas\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Registro de auditoria do Kanban. */
class HistoricoResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_historico,
            'statusAnterior' => $this->status_anterior,
            'statusNovo' => $this->status_novo,
            'dataAlteracao' => (string) $this->data_alteracao,
            'idUsuario' => (int) $this->id_usuario,
            'observacao' => $this->observacao,
        ];
    }
}
