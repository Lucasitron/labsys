<?php

namespace App\Modules\Vendas\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Orçamento (lista) + detalhe com itens. */
class OrcamentoResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        $base = [
            'id' => (int) $this->id_orcamento,
            'clienteId' => (int) $this->id_cliente,
            'clienteNome' => $this->cliente?->nome_razao_social,
            'dataCriacao' => substr((string) $this->data_criacao, 0, 10),
            'validade' => $this->validade === null ? null : substr((string) $this->validade, 0, 10),
            'valorTotal' => (string) $this->valor_total,
            'status' => $this->status->value,
            'criadoPor' => (int) $this->criado_por,
        ];

        if ($this->relationLoaded('itens')) {
            $base['observacoes'] = $this->observacoes;
            $base['itens'] = ItemOrcamentoResource::collection($this->itens)->toArray($request);
        } else {
            $base['qtdItens'] = (int) ($this->itens_count ?? 0);
        }

        return $base;
    }
}
