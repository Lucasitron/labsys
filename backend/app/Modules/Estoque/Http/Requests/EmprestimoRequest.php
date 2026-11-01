<?php

namespace App\Modules\Estoque\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** Empréstimo: devolução prevista hoje ou futura. */
class EmprestimoRequest extends FormRequest
{
    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'idItem' => ['required', 'integer'],
            'idPessoa' => ['required', 'integer'],
            'quantidade' => ['required', 'numeric', 'min:0.01'],
            'dataDevolucaoPrevista' => ['required', 'date', 'after_or_equal:today'],
            'observacao' => ['nullable', 'string', 'max:255'],
            'responsavel' => ['nullable', 'string', 'max:255'],
        ];
    }
}
