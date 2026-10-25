<?php

namespace App\Modules\Vendas\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** Interação do CRM (timeline por cliente). */
class InteracaoRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'clienteId' => ['required', 'integer'],
            'tipo' => ['required', 'string', 'in:E-mail,Telefone,Reunião,WhatsApp'],
            'descricao' => ['required', 'string', 'max:2000'],
            'dataInteracao' => ['nullable', 'date'],
        ];
    }
}
