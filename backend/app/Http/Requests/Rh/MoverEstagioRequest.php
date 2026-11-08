<?php

namespace App\Http\Requests\Rh;

use Illuminate\Foundation\Http\FormRequest;

/** PATCH processo-seletivo/{id}/estagio. */
class MoverEstagioRequest extends FormRequest
{
    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'etapa' => ['required', 'in:INSCRITO,EM_TRIAGEM,ENTREVISTA,APROVADO,REPROVADO'],
        ];
    }
}
