<?php

namespace App\Modules\Financeiro\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

class CategoriaRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'nome' => ['required', 'string', 'max:120'],
            'tipo' => ['required', 'string', 'in:RECEITA,DESPESA'],
            'descricao' => ['nullable', 'string', 'max:500'],
        ];
    }
}
