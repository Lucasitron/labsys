<?php

namespace App\Modules\Producao\Http\Requests;

use App\Modules\Auth\Models\Login;
use App\Modules\Producao\Policies\ProducaoPolicy;
use App\Modules\Producao\ProducaoPrincipal;
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
        ProducaoPolicy::exigeLeitura(ProducaoPrincipal::from($login));

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
