<?php

namespace App\Modules\Financeiro\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

class DoacaoRequest extends FormRequest
{
    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'tipo' => ['required', 'string', 'in:DOACAO,PROJETO'],
            'origem' => ['required', 'string', 'max:200'],
            'valor' => ['required', 'numeric', 'min:0.01'],
            'dataRecebimento' => ['required', 'date'],
            'idProjetoAssociado' => ['nullable', 'integer'],
        ];
    }
}
