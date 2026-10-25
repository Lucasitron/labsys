<?php

namespace App\Modules\Rh\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

class ProcessoResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id,
            'idCandidato' => (int) $this->id_candidato,
            'nomeCandidato' => $this->candidato?->nome_completo,
            'idTutor' => (int) $this->id_tutor,
            'statusProcesso' => $this->status_processo->value,
            'dataInscricao' => substr((string) $this->data_inscricao, 0, 10),
            'resultadoFinal' => $this->resultado_final,
            'idGrupo' => $this->id_grupo !== null ? (int) $this->id_grupo : null,
            'nota' => $this->nota !== null ? number_format((float) $this->nota, 2, '.', '') : null,
            'feedback' => $this->feedback,
        ];
    }
}
