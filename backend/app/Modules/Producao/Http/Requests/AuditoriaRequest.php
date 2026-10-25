<?php

namespace App\Modules\Producao\Http\Requests;

use App\Modules\Auth\Models\Login;
use App\Modules\Producao\Policies\ProducaoPolicy;
use App\Modules\Producao\ProducaoPrincipal;
use Illuminate\Foundation\Http\FormRequest;

/** Auditoria manual de projeto de mesa (F8, Admin — P3). */
class AuditoriaRequest extends FormRequest
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
            'idProjetoMesa' => ['required', 'integer'],
            'resultado' => ['required', 'string', 'in:ATIVO,ABANDONADO,CONCLUIDO'],
            'acaoTomada' => ['nullable', 'string', 'max:500'],
        ];
    }
}
