<?php

namespace App\Modules\Estoque\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** Item (criar/atualizar): autorização é do service (RBAC por vínculo). */
class ItemRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'nome' => ['required', 'string', 'max:255'],
            'descricao' => ['nullable', 'string', 'max:255'],
            'categoria' => ['required', 'string', 'in:INSUMO,FERRAMENTA,PECA'],
            'unidadeMedida' => ['required', 'string', 'max:32'],
            'quantidadeAtual' => ['required', 'numeric', 'min:0'],
            'estoqueMinimo' => ['required', 'numeric', 'min:0'],
            'idLocalizacao' => ['nullable', 'integer'],
        ];
    }
}
