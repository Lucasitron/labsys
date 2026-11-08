<?php

namespace App\Http\Requests\Producao;

use App\Models\Auth\Login;
use App\Policies\ProducaoPolicy;
use App\Shared\Auth\Caller;
use Illuminate\Foundation\Http\FormRequest;

/** Cadastro/atualização de setor 5S (F5). */
class SetorRequest extends FormRequest
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
            'numero' => ['required', 'integer'],
            'nome' => ['required', 'string', 'max:150'],
            'descricao' => ['nullable', 'string', 'max:1000'],
            'observacoes' => ['nullable', 'string', 'max:1000'],
            'fotoCorretoUrl' => ['nullable', 'string', 'max:500'],
            'fotoIncorretoUrl' => ['nullable', 'string', 'max:500'],
            'ativo' => ['nullable', 'boolean'],
        ];
    }
}
