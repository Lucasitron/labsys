<?php

namespace App\Modules\Vendas\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Registro de marketplace com bruto/líquido (dado p/ Financeiro). */
class MarketplaceResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        $bruto = (float) ($this->encomenda?->valor_final ?? 0);

        return [
            'id' => (int) $this->id_registro,
            'idEncomenda' => (int) $this->id_encomenda,
            'codigo' => 'EN-'.$this->id_encomenda,
            'plataforma' => $this->plataforma,
            'codigoExterno' => $this->codigo_externo,
            'clienteNome' => $this->encomenda?->cliente?->nome_razao_social,
            'valorBruto' => number_format($bruto, 2, '.', ''),
            'valorTaxa' => (string) $this->valor_taxa,
            'valorLiquido' => number_format($bruto - (float) $this->valor_taxa, 2, '.', ''),
            'dataVenda' => substr((string) $this->data_venda, 0, 10),
        ];
    }
}
