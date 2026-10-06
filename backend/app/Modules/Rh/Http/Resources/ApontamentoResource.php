<?php

namespace App\Modules\Rh\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

class ApontamentoResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id,
            'idFuncionario' => (int) $this->id_funcionario,
            'tipo' => $this->tipo->value,
            'idReferencia' => (int) $this->id_referencia,
            'data' => substr((string) $this->data, 0, 10),
            'horasTrabalhadas' => number_format((float) $this->horas_trabalhadas, 2, '.', ''),
            'horaInicio' => $this->hora_inicio !== null ? substr((string) $this->hora_inicio, 0, 5) : null,
            'horaFim' => $this->hora_fim !== null ? substr((string) $this->hora_fim, 0, 5) : null,
            'descricaoAtividade' => $this->descricao_atividade,
            'status' => $this->status->value,
            'motivoRejeicao' => $this->motivo_rejeicao,
            'idAdminValidador' => $this->id_admin_validador !== null ? (int) $this->id_admin_validador : null,
            'dataValidacao' => $this->data_validacao?->toIso8601String(),
        ];
    }
}
