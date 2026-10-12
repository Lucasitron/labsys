<?php

namespace App\Modules\Rh\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** POST /api/rh/processo-seletivo — candidato (vira pessoa + funcionário nível 4). */
class IniciarProcessoRequest extends FormRequest
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
            'turno' => ['nullable', 'string', 'max:32'],
            'idTutor' => ['nullable', 'integer'],
        ];
    }
}
