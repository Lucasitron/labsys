<?php

namespace App\Http\Requests\Estoque;

use Illuminate\Foundation\Http\FormRequest;

/** Consumo real dos itens da BOM. */
class BomConsumoRequest extends FormRequest
{
    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'itens' => ['required', 'array', 'min:1'],
            'itens.*.idItem' => ['required', 'integer'],
            'itens.*.quantidadeConsumida' => ['required', 'numeric', 'min:0.01'],
        ];
    }
}
