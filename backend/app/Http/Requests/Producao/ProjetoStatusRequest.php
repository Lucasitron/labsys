<?php

namespace App\Http\Requests\Producao;

use App\Models\Auth\Login;
use App\Policies\ProducaoPolicy;
use App\Shared\Auth\Caller;
use Illuminate\Foundation\Http\FormRequest;

/** Alteração de status de projeto (`CONCLUIDO` carimba `data_fim_real`). */
class ProjetoStatusRequest extends FormRequest
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
            'status' => ['required', 'string', 'in:PLANEJADO,EM_ANDAMENTO,CONCLUIDO,CANCELADO'],
            'dataFimReal' => ['nullable', 'date'],
        ];
    }
}
