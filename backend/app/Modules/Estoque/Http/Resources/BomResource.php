<?php

namespace App\Modules\Estoque\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** BOM com itens (previsto × real). */
class BomResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_bom,
            'idProdutoServico' => (int) $this->id_produto_servico,
            'nome' => $this->nome,
            'versao' => (int) $this->versao,
            'editavel' => (bool) $this->editavel,
            'itens' => collect($this->itens)->map(fn ($i) => [
                'idItem' => (int) $i->id_item,
                'quantidadePrevista' => (string) $i->quantidade_prevista,
                'quantidadeReal' => $i->quantidade_real === null ? null : (string) $i->quantidade_real,
            ])->all(),
        ];
    }
}
