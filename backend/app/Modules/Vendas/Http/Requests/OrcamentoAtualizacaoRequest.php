<?php

namespace App\Modules\Vendas\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** Ajustes de orçamento (status e/ou itens). Conversão é o POST /encomendas. */
class OrcamentoAtualizacaoRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'validade' => ['nullable', 'date'],
            'observacoes' => ['nullable', 'string', 'max:1000'],
            'status' => ['nullable', 'string', 'in:Pendente,Aprovado,Recusado,Ajuste'],
            'itens' => ['nullable', 'array', 'min:1'],
            'itens.*.descricao' => ['required', 'string', 'max:500'],
            'itens.*.quantidade' => ['required', 'numeric', 'min:0.01'],
            'itens.*.valorUnitario' => ['required', 'numeric', 'min:0'],
            'itens.*.materialTipo' => ['nullable', 'string', 'max:64'],
            'itens.*.materialQuantidade' => ['nullable', 'numeric', 'min:0'],
            'itens.*.materialUnidade' => ['nullable', 'string', 'max:16'],
            'itens.*.horas' => ['nullable', 'numeric', 'min:0'],
            'itens.*.compra' => ['nullable', 'boolean'],
            'itens.*.idItemEstoque' => ['nullable', 'integer'],
        ];
    }
}
