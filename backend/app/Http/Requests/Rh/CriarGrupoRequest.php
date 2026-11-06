<?php

namespace App\Http\Requests\Rh;

use Illuminate\Foundation\Http\FormRequest;

/** POST processo-seletivo/grupos — líder deve ser tutor. */
class CriarGrupoRequest extends FormRequest
{
    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'nome' => ['required', 'string', 'max:255'],
            'idLider' => ['required', 'integer'],
            'membroIds' => ['nullable', 'array'],
            'membroIds.*' => ['integer'],
        ];
    }
}
