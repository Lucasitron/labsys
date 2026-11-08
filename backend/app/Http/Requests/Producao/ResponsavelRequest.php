<?php

namespace App\Http\Requests\Producao;

use App\Models\Auth\Login;
use App\Policies\ProducaoPolicy;
use App\Shared\Auth\Caller;
use Illuminate\Foundation\Http\FormRequest;

/** Designação de responsável do setor (Admin — P3; rotação no service). */
class ResponsavelRequest extends FormRequest
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
            'idFuncionario' => ['required', 'integer'],
            'dataInicio' => ['required', 'date'],
            'dataFim' => ['nullable', 'date'],
            'ativo' => ['nullable', 'boolean'],
        ];
    }
}
