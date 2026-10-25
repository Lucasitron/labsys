<?php

namespace App\Modules\Financeiro\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

class CustoResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_custo,
            'idEncomenda' => (int) $this->id_encomenda,
            'custoMateriais' => (string) $this->custo_materiais,
            'custoMaoObra' => (string) $this->custo_mao_obra,
            'custoOverhead' => (string) $this->custo_overhead,
            'custoTotal' => (string) $this->custo_total,
            'valorVenda' => (string) $this->valor_venda,
            'margemLucro' => (string) $this->margem_lucro,
            'dataCalculo' => substr((string) $this->data_calculo, 0, 10),
        ];
    }
}
