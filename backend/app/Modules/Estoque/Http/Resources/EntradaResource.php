<?php

namespace App\Modules\Estoque\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Entrada de estoque. */
class EntradaResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_entrada,
            'idItem' => (int) $this->id_item,
            'nomeItem' => $this->item?->nome,
            'idFornecedor' => (int) $this->id_fornecedor,
            'nomeFornecedor' => $this->fornecedor?->nome,
            'quantidade' => (string) $this->quantidade,
            'valorUnitario' => (string) $this->valor_unitario,
            'valorTotal' => (string) $this->valor_total,
            'dataEntrada' => substr((string) $this->data_entrada, 0, 10),
            'notaFiscal' => $this->nota_fiscal,
            'observacao' => $this->observacao,
            'responsavel' => $this->responsavel,
        ];
    }
}
