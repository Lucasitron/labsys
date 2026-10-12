<?php

namespace App\Modules\Financeiro\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

class LancamentoResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_lancamento,
            'idCategoria' => (int) $this->id_categoria,
            'tipo' => $this->tipo->value,
            'valor' => (string) $this->valor,
            'dataVencimento' => substr((string) $this->data_vencimento, 0, 10),
            'dataPagamento' => $this->data_pagamento === null
                ? null : substr((string) $this->data_pagamento, 0, 10),
            'status' => $this->status->value,
            'idReferenciaExterna' => $this->id_referencia_externa,
            'observacao' => $this->observacao,
        ];
    }
}
