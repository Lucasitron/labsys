<?php

namespace App\Modules\Vendas\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Item de orçamento com subtotal. */
class ItemOrcamentoResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        $subtotal = (float) $this->quantidade * (float) $this->valor_unitario;

        return [
            'id' => (int) $this->id_item_orcamento,
            'descricao' => $this->descricao,
            'quantidade' => (string) $this->quantidade,
            'valorUnitario' => (string) $this->valor_unitario,
            'subtotal' => number_format($subtotal, 2, '.', ''),
            'materialTipo' => $this->material_tipo,
            'materialQuantidade' => $this->material_quantidade === null ? null : (string) $this->material_quantidade,
            'materialUnidade' => $this->material_unidade,
            'horas' => $this->horas === null ? null : (string) $this->horas,
            'compra' => (bool) $this->compra,
        ];
    }
}
