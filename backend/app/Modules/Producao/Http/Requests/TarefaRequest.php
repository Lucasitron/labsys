<?php

namespace App\Modules\Producao\Http\Requests;

use App\Modules\Auth\Models\Login;
use App\Modules\Producao\Policies\ProducaoPolicy;
use App\Modules\Producao\ProducaoPrincipal;
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
        ProducaoPolicy::exigeLeitura(ProducaoPrincipal::from($login));

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
