<?php

namespace App\Modules\Rh\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** PUT validar — status VALIDADO|REJEITADO (motivo exigido no service p/ rejeitar). */
class ValidarApontamentoRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'status' => ['required', 'in:VALIDADO,REJEITADO'],
            'motivo' => ['nullable', 'string', 'max:500'],
        ];
    }
}
