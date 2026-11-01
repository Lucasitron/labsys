<?php

namespace App\Modules\Financeiro\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** Liquidação: corpo opcional (data default hoje). */
class PagamentoRequest extends FormRequest
{
    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'dataPagamento' => ['nullable', 'date'],
            'observacao' => ['nullable', 'string', 'max:1000'],
        ];
    }
}
