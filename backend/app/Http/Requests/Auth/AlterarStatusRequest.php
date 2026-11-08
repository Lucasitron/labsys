<?php

namespace App\Http\Requests\Auth;

use Illuminate\Foundation\Http\FormRequest;

/** PATCH /api/usuarios/{id}/status — ativa/pende/desativa conta. */
class AlterarStatusRequest extends FormRequest
{
    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'situacao' => ['required', 'string', 'max:20'],
        ];
    }
}
