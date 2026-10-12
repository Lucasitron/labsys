<?php

namespace App\Modules\Producao\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Tarefa. */
class TarefaResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_tarefa,
            'idProjeto' => (int) $this->id_projeto,
            'titulo' => $this->titulo,
            'descricao' => $this->descricao,
            'idResponsavel' => $this->id_responsavel === null ? null : (int) $this->id_responsavel,
            'dataInicio' => $this->data_inicio === null
                ? null : substr((string) $this->data_inicio, 0, 10),
            'dataFimPrevista' => $this->data_fim_prevista === null
                ? null : substr((string) $this->data_fim_prevista, 0, 10),
            'dataConclusao' => $this->data_conclusao === null
                ? null : substr((string) $this->data_conclusao, 0, 10),
            'status' => $this->status->value,
            'prioridade' => $this->prioridade->value,
        ];
    }
}
