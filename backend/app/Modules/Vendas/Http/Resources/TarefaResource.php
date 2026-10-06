<?php

namespace App\Modules\Vendas\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Tarefa de marketing. */
class TarefaResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_tarefa,
            'titulo' => $this->titulo,
            'descricao' => $this->descricao,
            'responsavelId' => (int) $this->id_responsavel,
            'dataInicio' => $this->data_inicio === null ? null : substr((string) $this->data_inicio, 0, 10),
            'dataFim' => $this->data_fim === null ? null : substr((string) $this->data_fim, 0, 10),
            'status' => $this->status,
            'prioridade' => $this->prioridade,
            'criadoPor' => (int) $this->criado_por,
        ];
    }
}
