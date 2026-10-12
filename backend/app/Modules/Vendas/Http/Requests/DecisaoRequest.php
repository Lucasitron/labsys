<?php

namespace App\Modules\Vendas\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** Decisão de solicitação (Admin-only; motivo obrigatório ao rejeitar — service). */
class DecisaoRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'decisao' => ['required', 'string'],
            'motivo' => ['nullable', 'string', 'max:1000'],
        ];
    }
}
