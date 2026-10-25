<?php

namespace App\Modules\Producao\Http\Requests;

use App\Modules\Auth\Models\Login;
use App\Modules\Producao\Policies\ProducaoPolicy;
use App\Modules\Producao\ProducaoPrincipal;
use Illuminate\Foundation\Http\FormRequest;

/** Movimentação do cartão (lock otimista: `version` divergente → 409). */
class KanbanMoverRequest extends FormRequest
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
            'statusNovo' => ['required', 'string', 'in:FILA,PRODUCAO,ACABAMENTO,PRONTO,ENTREGUE'],
            'version' => ['required', 'integer'],
            'observacao' => ['nullable', 'string', 'max:500'],
        ];
    }
}
