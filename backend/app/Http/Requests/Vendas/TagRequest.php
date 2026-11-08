<?php

namespace App\Http\Requests\Vendas;

use Illuminate\Foundation\Http\FormRequest;

/** Tag de cliente (CRUD trivial, sem service). */
class TagRequest extends FormRequest
{
    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'nome' => ['required', 'string', 'max:64'],
            'cor' => ['nullable', 'string', 'max:16'],
        ];
    }
}
