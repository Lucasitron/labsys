<?php

namespace App\Modules\Rh\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** POST /api/rh/niveis/convites — pessoa + funcionário (nível 4 nasce Recrutando). Admin. */
class ConvidarRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'nomeCompleto' => ['required', 'string', 'max:255'],
            'matricula' => ['required', 'string', 'max:64'],
            'contato' => ['nullable', 'string', 'max:255'],
            'departamento' => ['nullable', 'string', 'max:255'],
            'nivelAcesso' => ['nullable', 'integer', 'min:0', 'max:4'],
        ];
    }
}
