<?php

namespace App\Modules\Estoque\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Fornecedor. */
class FornecedorResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_fornecedor,
            'nome' => $this->nome,
            'contato' => $this->contato,
            'cnpj' => $this->cnpj,
        ];
    }
}
