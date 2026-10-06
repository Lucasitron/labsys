<?php

namespace App\Modules\Vendas\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/**
 * Encomenda: conversão (`idOrcamento` aprovado) ou venda direta
 * (`clienteId` + `valorFinal`) — sem endpoint `converter` próprio.
 */
class EncomendaRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'idOrcamento' => ['nullable', 'integer'],
            'clienteId' => ['nullable', 'integer'],
            'valorFinal' => ['nullable', 'numeric', 'min:0'],
            'dataPrevisaoEntrega' => ['nullable', 'date'],
            'observacoes' => ['nullable', 'string', 'max:1000'],
        ];
    }
}
