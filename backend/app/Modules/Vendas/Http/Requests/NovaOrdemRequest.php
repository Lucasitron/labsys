<?php

namespace App\Modules\Vendas\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** Nova ordem por alteração (encerra a atual + cria nova com origem). */
class NovaOrdemRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'valorFinal' => ['required', 'numeric', 'min:0'],
            'dataPrevisaoEntrega' => ['nullable', 'date'],
            'observacoes' => ['nullable', 'string', 'max:1000'],
        ];
    }
}
