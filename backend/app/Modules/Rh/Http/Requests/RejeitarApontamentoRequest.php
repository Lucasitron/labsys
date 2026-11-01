<?php

namespace App\Modules\Rh\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** PATCH horas/{id}/rejeitar — motivo obrigatório. */
class RejeitarApontamentoRequest extends FormRequest
{
    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'motivo' => ['required', 'string', 'max:500'],
        ];
    }
}
