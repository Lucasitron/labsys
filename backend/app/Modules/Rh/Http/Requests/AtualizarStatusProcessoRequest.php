<?php

namespace App\Modules\Rh\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** PUT processo-seletivo/{id} — evolução de estágio + resultado. */
class AtualizarStatusProcessoRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'statusProcesso' => ['required', 'in:INSCRITO,EM_TRIAGEM,ENTREVISTA,APROVADO,REPROVADO'],
            'resultadoFinal' => ['nullable', 'string', 'max:255'],
        ];
    }
}
