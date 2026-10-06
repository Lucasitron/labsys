<?php

namespace App\Modules\Estoque\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** Saída manual (consumo, perda, ajuste). */
class SaidaRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'idItem' => ['required', 'integer'],
            'quantidade' => ['required', 'numeric', 'min:0.01'],
            'tipoSaida' => ['required', 'string', 'in:CONSUMO,PERDA,AJUSTE,EMPRESTIMO'],
            'idReferencia' => ['nullable', 'integer'],
            'observacao' => ['nullable', 'string', 'max:255'],
            'responsavel' => ['nullable', 'string', 'max:255'],
        ];
    }
}
