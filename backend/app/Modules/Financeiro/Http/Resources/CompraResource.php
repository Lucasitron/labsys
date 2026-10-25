<?php

namespace App\Modules\Financeiro\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

class CompraResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_solicitacao,
            'idItemEstoque' => (int) $this->id_item_estoque,
            'quantidade' => (string) $this->quantidade,
            'valorEstimado' => $this->valor_estimado === null ? null : (string) $this->valor_estimado,
            'status' => $this->status->value,
            'dataSolicitacao' => substr((string) $this->data_solicitacao, 0, 10),
        ];
    }
}
