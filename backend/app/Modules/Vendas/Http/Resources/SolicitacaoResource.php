<?php

namespace App\Modules\Vendas\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Solicitação de edição (D-4: aprovada não altera o alvo). */
class SolicitacaoResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_solicitacao,
            'tipo' => $this->tipo->value,
            'alvoTipo' => $this->alvo_tipo->value,
            'alvoId' => (int) $this->alvo_id,
            'campo' => $this->campo,
            'valorAtual' => $this->valor_atual,
            'valorProposto' => $this->valor_proposto,
            'justificativa' => $this->justificativa,
            'status' => $this->status->value,
            'solicitanteId' => (int) $this->solicitante_id,
            'decididoPor' => $this->decidido_por === null ? null : (int) $this->decidido_por,
            'motivoDecisao' => $this->motivo_decisao,
            'dataCriacao' => (string) $this->data_criacao,
            'dataDecisao' => $this->data_decisao === null ? null : (string) $this->data_decisao,
        ];
    }
}
