<?php

namespace App\Modules\Financeiro\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

class FechamentoRequest extends FormRequest
{
    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'idEncomenda' => ['required', 'integer'],
            'horasEstimadas' => ['required', 'numeric', 'min:0'],
            'valorFechado' => ['required', 'numeric', 'min:0'],
            'dataFechamento' => ['nullable', 'date'],
        ];
    }
}
