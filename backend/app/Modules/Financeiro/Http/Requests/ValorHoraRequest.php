<?php

namespace App\Modules\Financeiro\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

class ValorHoraRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'nivelAcesso' => ['required', 'integer', 'min:0', 'max:3'],
            'valorHora' => ['required', 'numeric', 'min:0'],
            'dataVigencia' => ['required', 'date'],
        ];
    }
}
