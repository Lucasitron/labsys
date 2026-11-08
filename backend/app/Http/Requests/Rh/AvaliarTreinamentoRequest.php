<?php

namespace App\Http\Requests\Rh;

use Illuminate\Foundation\Http\FormRequest;

/** POST treinamentos/{id}/avaliacoes — nota 0-10 + feedback. */
class AvaliarTreinamentoRequest extends FormRequest
{
    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'idFuncionario' => ['required', 'integer'],
            'nota' => ['required', 'numeric', 'min:0', 'max:10'],
            'feedback' => ['nullable', 'string'],
        ];
    }
}
