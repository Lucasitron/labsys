<?php

namespace App\Modules\Estoque\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** Localização física (só Admin cadastra — rota com `can:admin`). */
class LocalizacaoRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'armario' => ['required', 'string', 'max:255'],
            'prateleira' => ['nullable', 'string', 'max:255'],
            'caixa' => ['nullable', 'string', 'max:255'],
            'descricao' => ['nullable', 'string', 'max:255'],
        ];
    }
}
