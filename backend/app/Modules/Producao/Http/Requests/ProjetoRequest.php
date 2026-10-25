<?php

namespace App\Modules\Producao\Http\Requests;

use App\Modules\Auth\Models\Login;
use App\Modules\Producao\Policies\ProducaoPolicy;
use App\Modules\Producao\ProducaoPrincipal;
use Illuminate\Foundation\Http\FormRequest;

/** Criação/atualização de projeto (F1). Vínculo do responsável no service. */
class ProjetoRequest extends FormRequest
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
            'nome' => ['required', 'string', 'max:150'],
            'descricao' => ['nullable', 'string', 'max:1000'],
            'dataInicio' => ['required', 'date'],
            'dataFimPrevista' => ['nullable', 'date'],
            'idResponsavel' => ['required', 'integer'],
            'status' => ['nullable', 'string', 'in:PLANEJADO,EM_ANDAMENTO,CONCLUIDO,CANCELADO'],
        ];
    }
}
