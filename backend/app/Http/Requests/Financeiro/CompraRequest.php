<?php

namespace App\Http\Requests\Financeiro;

use Illuminate\Foundation\Http\FormRequest;

class CompraRequest extends FormRequest
{
    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'idItemEstoque' => ['required', 'integer'],
            'quantidade' => ['required', 'numeric', 'min:0.01'],
            'valorEstimado' => ['nullable', 'numeric', 'min:0'],
        ];
    }
}
