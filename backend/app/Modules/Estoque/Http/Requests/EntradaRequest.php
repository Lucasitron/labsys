<?php

namespace App\Modules\Estoque\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** Entrada / compra simples. */
class EntradaRequest extends FormRequest
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
            'idFornecedor' => ['required', 'integer'],
            'quantidade' => ['required', 'numeric', 'min:0.01'],
            'valorUnitario' => ['required', 'numeric', 'min:0'],
            'dataEntrada' => ['nullable', 'date'],
            'notaFiscal' => ['nullable', 'string', 'max:255'],
            'observacao' => ['nullable', 'string', 'max:255'],
            'responsavel' => ['nullable', 'string', 'max:255'],
        ];
    }
}
