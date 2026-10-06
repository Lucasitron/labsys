<?php

namespace App\Modules\Estoque\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** Fornecedor (só Admin cadastra — rota com `can:admin`). */
class FornecedorRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'nome' => ['required', 'string', 'max:255'],
            'contato' => ['nullable', 'string', 'max:255'],
            'cnpj' => ['nullable', 'string', 'max:32'],
        ];
    }
}
