<?php

namespace App\Modules\Auth\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** PUT /api/configuracoes/sistema — identidade + cadências 5S. */
class AtualizarSistemaRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'identidade' => ['required', 'array'],
            'identidade.nomeFablab' => ['required', 'string', 'max:255'],
            'identidade.logo' => ['nullable', 'string', 'max:2048'],
            'cadenciaChecklist5S' => ['required', 'string', 'max:32'],
            'cadenciaAuditoria5S' => ['nullable', 'string', 'max:32'],
        ];
    }
}
