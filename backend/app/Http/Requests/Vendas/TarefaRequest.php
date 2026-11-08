<?php

namespace App\Http\Requests\Vendas;

use Illuminate\Foundation\Http\FormRequest;

/** Tarefa de marketing (status/prioridade com default no service). */
class TarefaRequest extends FormRequest
{
    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'titulo' => ['required', 'string', 'max:255'],
            'descricao' => ['nullable', 'string', 'max:2000'],
            'responsavelId' => ['required', 'integer'],
            'dataInicio' => ['nullable', 'date'],
            'dataFim' => ['nullable', 'date'],
            'status' => ['nullable', 'string', 'in:Pendente,Em Andamento,Concluída'],
            'prioridade' => ['nullable', 'string', 'in:Baixa,Média,Alta'],
        ];
    }
}
