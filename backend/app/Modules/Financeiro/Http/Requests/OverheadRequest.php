<?php

namespace App\Modules\Financeiro\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

class OverheadRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'valorTaxaHora' => ['required', 'numeric', 'min:0'],
            'dataVigencia' => ['required', 'date'],
        ];
    }
}
