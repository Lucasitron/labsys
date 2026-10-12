<?php

namespace App\Modules\Vendas\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** Tag de cliente (CRUD trivial, sem service). */
class TagRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'nome' => ['required', 'string', 'max:64'],
            'cor' => ['nullable', 'string', 'max:16'],
        ];
    }
}
