<?php

namespace App\Modules\Vendas\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** Pedido de edição (qualquer autenticado exceto Recrutando — service). */
class SolicitacaoRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'tipo' => ['required', 'string', 'in:ALTERACAO_DADOS,MUDANCA_STATUS,MOVER_ENCOMENDA,OUTRA'],
            'alvoTipo' => ['required', 'string', 'in:CLIENTE,ORCAMENTO,ENCOMENDA'],
            'alvoId' => ['required', 'integer'],
            'campo' => ['required', 'string', 'max:128'],
            'valorAtual' => ['nullable', 'string', 'max:1000'],
            'valorProposto' => ['required', 'string', 'max:1000'],
            'justificativa' => ['required', 'string', 'max:1000'],
        ];
    }
}
