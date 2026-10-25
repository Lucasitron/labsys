<?php

namespace App\Modules\Auth\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** POST /api/configuracoes/tokens — nova integração. */
class CriarTokenRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'nome' => ['required', 'string', 'max:128'],
        ];
    }
}
