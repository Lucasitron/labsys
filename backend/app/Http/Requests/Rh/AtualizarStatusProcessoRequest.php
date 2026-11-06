<?php

namespace App\Http\Requests\Rh;

use Illuminate\Foundation\Http\FormRequest;

/** PUT processo-seletivo/{id} — evolução de estágio + resultado. */
class AtualizarStatusProcessoRequest extends FormRequest
{
    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'statusProcesso' => ['required', 'in:INSCRITO,EM_TRIAGEM,ENTREVISTA,APROVADO,REPROVADO'],
            'resultadoFinal' => ['nullable', 'string', 'max:255'],
        ];
    }
}
