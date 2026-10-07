<?php

namespace App\Modules\Producao\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Projeto. */
class ProjetoResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_projeto,
            'nome' => $this->nome,
            'descricao' => $this->descricao,
            'dataInicio' => substr((string) $this->data_inicio, 0, 10),
            'dataFimPrevista' => $this->data_fim_prevista === null
                ? null : substr((string) $this->data_fim_prevista, 0, 10),
            'dataFimReal' => $this->data_fim_real === null
                ? null : substr((string) $this->data_fim_real, 0, 10),
            'status' => $this->status->value,
            'idResponsavel' => (int) $this->id_responsavel,
        ];
    }
}
