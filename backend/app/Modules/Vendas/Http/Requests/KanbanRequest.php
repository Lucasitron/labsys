<?php

namespace App\Modules\Vendas\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** Mover o cartão no Kanban (lock otimista: `versao` divergente → 409). */
class KanbanRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'statusKanban' => ['required', 'string', 'in:Fila,Produção,Acabamento,Pronto,Entregue'],
            'versao' => ['required', 'integer'],
            'observacao' => ['nullable', 'string', 'max:1000'],
        ];
    }
}
