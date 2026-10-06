<?php

namespace App\Modules\Auth\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** POST /api/usuarios — convite de conta (situação ausente = PENDENTE, nível ausente = 4). */
class CriarUsuarioRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'idUser' => ['required', 'integer'],
            'email' => ['required', 'email', 'max:255'],
            'nomeUsuario' => ['required', 'string', 'max:255'],
            'senha' => ['required', 'string', 'min:8', 'max:72'],
            'uuid' => ['nullable', 'string', 'max:255'],
            'setor' => ['nullable', 'string', 'max:255'],
            'nivel' => ['nullable', 'integer', 'min:0', 'max:4'],
            'situacao' => ['nullable', 'string', 'max:20'],
        ];
    }
}
