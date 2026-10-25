<?php

namespace App\Modules\Auth\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** PUT /api/auth/senha — troca a própria senha (nova 8-72 + confirmação). */
class AlterarSenhaRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'senhaAtual' => ['required', 'string'],
            'novaSenha' => ['required', 'string', 'min:8', 'max:72'],
            'confirmacaoSenha' => ['required', 'string'],
        ];
    }
}
