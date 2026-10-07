<?php

namespace App\Modules\Notification\Http\Requests;

use App\Modules\Auth\Models\Login;
use Illuminate\Foundation\Http\FormRequest;

/**
 * Matriz `tipo × canal` (Admin). A validação estrita (tipos/canais/bools,
 * Java 1:1) vive no service → 400; aqui só autorização (regras vazias de
 * propósito — validar no Request daria 422 e quebraria o contrato D-5).
 */
class AtualizarPreferenciasRequest extends FormRequest
{
    public function authorize(): bool
    {
        return $this->user() instanceof Login;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [];
    }

    /** @return array<string,array<string,bool|null>> */
    public function matriz(): array
    {
        $dado = $this->all();

        return is_array($dado) ? $dado : [];
    }
}
