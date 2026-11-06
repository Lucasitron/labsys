<?php

namespace App\Http\Requests\Auth;

use Illuminate\Foundation\Http\FormRequest;

/** POST /api/configuracoes/tokens — nova integração. */
class CriarTokenRequest extends FormRequest
{
    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'nome' => ['required', 'string', 'max:128'],
        ];
    }
}
