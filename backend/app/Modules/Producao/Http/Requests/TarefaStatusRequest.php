<?php

namespace App\Modules\Producao\Http\Requests;

use App\Models\Auth\Login;
use App\Modules\Producao\Policies\ProducaoPolicy;
use App\Shared\Auth\Caller;
use Illuminate\Foundation\Http\FormRequest;

/** Alteração de status de tarefa (`CONCLUIDA` carimba `data_conclusao`). */
class TarefaStatusRequest extends FormRequest
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
            'status' => ['required', 'string', 'in:PENDENTE,EM_ANDAMENTO,CONCLUIDA,ATRASADA'],
            'dataConclusao' => ['nullable', 'date'],
        ];
    }
}
