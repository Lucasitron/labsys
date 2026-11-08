<?php

namespace App\Http\Requests\Auth;

use Illuminate\Foundation\Http\FormRequest;

/** POST /api/auth/login — e-mail ou nome de usuário + senha (6-72). */
class LoginRequest extends FormRequest
{
    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'email' => ['nullable', 'required_without:nomeUsuario', 'email', 'max:255'],
            'nomeUsuario' => ['nullable', 'required_without:email', 'string', 'max:255'],
            'senha' => ['required', 'string', 'min:6', 'max:72'],
        ];
    }
}
