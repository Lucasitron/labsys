<?php

namespace App\Modules\Financeiro\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

class LancamentoRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'idCategoria' => ['required', 'integer'],
            'tipo' => ['required', 'string', 'in:ENTRADA,SAIDA'],
            'valor' => ['required', 'numeric', 'min:0.01'],
            'dataVencimento' => ['required', 'date'],
            'idReferenciaExterna' => ['nullable', 'string', 'max:100'],
            'observacao' => ['nullable', 'string', 'max:1000'],
        ];
    }
}
