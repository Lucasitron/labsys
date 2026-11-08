<?php

namespace App\Http\Requests\Rh;

use Illuminate\Foundation\Http\FormRequest;

/** POST /api/rh/funcionarios — vínculo (nível ausente = BOLSISTA). Admin. */
class VincularFuncionarioRequest extends FormRequest
{
    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'idPessoa' => ['required', 'integer'],
            'nivelAcesso' => ['nullable', 'integer', 'min:0', 'max:4'],
            'departamento' => ['nullable', 'string', 'max:255'],
        ];
    }
}
