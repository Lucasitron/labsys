<?php

namespace App\Http\Requests\Producao;

use App\Models\Auth\Login;
use App\Policies\ProducaoPolicy;
use App\Shared\Auth\Caller;
use Illuminate\Foundation\Http\FormRequest;

/** Material esperado do setor (F5). */
class MaterialRequest extends FormRequest
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
            'descricao' => ['required', 'string', 'max:255'],
            'quantidade' => ['required', 'numeric', 'min:0'],
        ];
    }
}
