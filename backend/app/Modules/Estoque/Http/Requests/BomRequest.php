<?php

namespace App\Modules\Estoque\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** BOM (criar/atualizar): ao menos um item. */
class BomRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'idProdutoServico' => ['required', 'integer'],
            'nome' => ['required', 'string', 'max:255'],
            'versao' => ['nullable', 'integer', 'min:1'],
            'editavel' => ['nullable', 'boolean'],
            'itens' => ['required', 'array', 'min:1'],
            'itens.*.idItem' => ['required', 'integer'],
            'itens.*.quantidadePrevista' => ['required', 'numeric', 'min:0.01'],
        ];
    }
}
