<?php

namespace App\Modules\Auth\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** PUT /api/usuarios/{id} — atualização parcial de conta. */
class AtualizarUsuarioRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'email' => ['nullable', 'email', 'max:255'],
            'nomeUsuario' => ['nullable', 'string', 'max:255'],
            'setor' => ['nullable', 'string', 'max:255'],
            'nivel' => ['nullable', 'integer', 'min:0', 'max:4'],
            'situacao' => ['nullable', 'string', 'max:20'],
        ];
    }
}
