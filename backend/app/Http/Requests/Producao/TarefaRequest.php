<?php

namespace App\Http\Requests\Producao;

use App\Models\Auth\Login;
use App\Policies\ProducaoPolicy;
use App\Shared\Auth\Caller;
use Illuminate\Foundation\Http\FormRequest;

/** Criação/atualização de tarefa (F2, sempre vinculada a um projeto). */
class TarefaRequest extends FormRequest
{
    public function authorize(): bool
    {
        $login = $this->user();

        if (! $login instanceof Login) {
            return false;
        }

        // RBAC antes da validação: Recrutando → 403 em tudo (convenção
        // "FormRequest valida+autoriza"; vínculos finos ficam no service).
        ProducaoPolicy::exigeLeitura(Caller::from($login));

        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'idProjeto' => ['required', 'integer'],
            'titulo' => ['required', 'string', 'max:150'],
            'descricao' => ['nullable', 'string', 'max:1000'],
            'idResponsavel' => ['nullable', 'integer'],
            'dataInicio' => ['nullable', 'date'],
            'dataFimPrevista' => ['nullable', 'date'],
            'prioridade' => ['nullable', 'string', 'in:BAIXA,MEDIA,ALTA'],
        ];
    }
}
