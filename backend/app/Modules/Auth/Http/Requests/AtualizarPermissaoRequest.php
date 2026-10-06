<?php

namespace App\Modules\Auth\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** PUT /api/permissoes/{modulo}/{nivel} — novo valor da célula (422 se inválido). */
class AtualizarPermissaoRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'valor' => ['required', 'string', 'max:16'],
        ];
    }
}
