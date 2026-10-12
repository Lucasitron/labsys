<?php

namespace App\Modules\Estoque\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Item com localização física completa. */
class ItemResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_item,
            'nome' => $this->nome,
            'descricao' => $this->descricao,
            'categoria' => $this->categoria->value,
            'unidadeMedida' => $this->unidade_medida,
            'quantidadeAtual' => (string) $this->quantidade_atual,
            'estoqueMinimo' => (string) $this->estoque_minimo,
            'localizacao' => $this->localizacao === null
                ? null
                : (new LocalizacaoResource($this->localizacao))->toArray($request),
        ];
    }
}
