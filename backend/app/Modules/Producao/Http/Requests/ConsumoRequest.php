<?php

namespace App\Modules\Producao\Http\Requests;

use App\Models\Auth\Login;
use App\Modules\Producao\Policies\ProducaoPolicy;
use App\Shared\Auth\Caller;
use Illuminate\Foundation\Http\FormRequest;

/** Registro da BOM final por encomenda (F11: upsert idempotente). */
class ConsumoRequest extends FormRequest
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
            'itens' => ['required', 'array', 'min:1'],
            'itens.*.idItem' => ['required', 'integer'],
            'itens.*.quantidade' => ['required', 'numeric', 'min:0.01'],
        ];
    }
}
