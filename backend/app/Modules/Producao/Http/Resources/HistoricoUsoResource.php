<?php

namespace App\Modules\Producao\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Registro de uso de máquina. */
class HistoricoUsoResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_uso,
            'idMaquina' => (int) $this->id_maquina,
            'idFuncionario' => (int) $this->id_funcionario,
            'dataInicio' => (string) $this->data_inicio,
            'dataFim' => $this->data_fim === null ? null : (string) $this->data_fim,
            'horasUso' => $this->horas_uso === null ? null : (string) $this->horas_uso,
            'observacao' => $this->observacao,
        ];
    }
}
