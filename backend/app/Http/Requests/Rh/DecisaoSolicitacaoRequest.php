<?php

namespace App\Http\Requests\Rh;

use Illuminate\Foundation\Http\FormRequest;

/** PUT solicitacoes/{id}/aprovar|rejeitar — observacao opcional. Admin. */
class DecisaoSolicitacaoRequest extends FormRequest
{
    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'observacao' => ['nullable', 'string'],
        ];
    }
}
