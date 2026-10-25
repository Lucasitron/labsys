<?php

namespace App\Modules\Producao\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Responsável do setor. */
class ResponsavelResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_responsavel,
            'idSetor' => (int) $this->id_setor,
            'idFuncionario' => (int) $this->id_funcionario,
            'dataInicio' => substr((string) $this->data_inicio, 0, 10),
            'dataFim' => $this->data_fim === null ? null : substr((string) $this->data_fim, 0, 10),
            'ativo' => (bool) $this->ativo,
        ];
    }
}
