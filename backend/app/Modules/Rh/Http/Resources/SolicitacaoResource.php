<?php

namespace App\Modules\Rh\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

class SolicitacaoResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'idSolicitacao' => (int) $this->id,
            'idFuncionario' => (int) $this->id_funcionario,
            'nomeFuncionario' => $this->funcionario?->pessoa?->nome_completo,
            'tipoCertificado' => $this->tipo_certificado->value,
            'dataSolicitacao' => $this->data_solicitacao->toIso8601String(),
            'horasSolicitadas' => number_format((float) $this->horas_solicitadas, 2, '.', ''),
            'status' => $this->status->value,
            'idAdminAprovador' => $this->id_admin_aprovador !== null ? (int) $this->id_admin_aprovador : null,
            'dataDecisao' => $this->data_decisao?->toIso8601String(),
            'observacao' => $this->observacao,
        ];
    }
}
