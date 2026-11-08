<?php

namespace App\Http\Requests\Vendas;

use Illuminate\Foundation\Http\FormRequest;

/** Venda manual externa vinculada a encomenda. */
class MarketplaceRequest extends FormRequest
{
    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'encomendaId' => ['required', 'integer'],
            'plataforma' => ['required', 'string', 'max:64'],
            'codigoExterno' => ['required', 'string', 'max:64'],
            'dataVenda' => ['required', 'date'],
            'valorTaxa' => ['required', 'numeric', 'min:0'],
        ];
    }
}
