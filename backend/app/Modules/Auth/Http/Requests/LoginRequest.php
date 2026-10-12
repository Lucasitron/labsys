<?php

namespace App\Modules\Auth\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** POST /api/auth/login — e-mail ou nome de usuário + senha (6-72). */
class LoginRequest extends FormRequest
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
            'senha' => ['required', 'string', 'min:6', 'max:72'],
        ];
    }
}
