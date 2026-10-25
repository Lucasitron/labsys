<?php

namespace App\Modules\Estoque\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** Consumo real dos itens da BOM. */
class BomConsumoRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

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
