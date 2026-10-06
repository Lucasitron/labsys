<?php

namespace App\Modules\Auth\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** PATCH /api/usuarios/{id}/status — ativa/pende/desativa conta. */
class AlterarStatusRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'situacao' => ['required', 'string', 'max:20'],
        ];
    }
}
