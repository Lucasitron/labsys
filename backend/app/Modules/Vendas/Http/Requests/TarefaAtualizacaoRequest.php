<?php

namespace App\Modules\Vendas\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** Atualização de tarefa (status/prioridade/prazo). */
class TarefaAtualizacaoRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'status' => ['nullable', 'string', 'in:Pendente,Em Andamento,Concluída'],
            'prioridade' => ['nullable', 'string', 'in:Baixa,Média,Alta'],
            'dataFim' => ['nullable', 'date'],
        ];
    }
}
