<?php

namespace App\Http\Requests\Rh;

use Illuminate\Foundation\Http\FormRequest;

/** PUT funcionarios/{id}/nivel + PATCH niveis/{id}/membros — campo único `nivel`. Admin. */
class AlterarNivelRequest extends FormRequest
{
    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'nivel' => ['required', 'integer', 'min:0', 'max:4'],
        ];
    }
}
