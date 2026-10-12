<?php

namespace App\Modules\Vendas\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** Regras de item de orçamento (reusadas em criação e ajuste). */
class OrcamentoRequest extends FormRequest
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
            'validade' => ['nullable', 'date'],
            'observacoes' => ['nullable', 'string', 'max:1000'],
            'itens' => ['required', 'array', 'min:1'],
            'itens.*.descricao' => ['required', 'string', 'max:500'],
            'itens.*.quantidade' => ['required', 'numeric', 'min:0.01'],
            'itens.*.valorUnitario' => ['required', 'numeric', 'min:0'],
            'itens.*.materialTipo' => ['nullable', 'string', 'max:64'],
            'itens.*.materialQuantidade' => ['nullable', 'numeric', 'min:0'],
            'itens.*.materialUnidade' => ['nullable', 'string', 'max:16'],
            'itens.*.horas' => ['nullable', 'numeric', 'min:0'],
            'itens.*.compra' => ['nullable', 'boolean'],
            // Transitório (sem coluna): só valida existência via EstoqueContract no service.
            'itens.*.idItemEstoque' => ['nullable', 'integer'],
        ];
    }
}
