<?php

namespace App\Modules\Rh\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** POST processo-seletivo/{id}/membros/{pessoaId}/avaliar — nota 0-10 + feedback. */
class AvaliarCandidatoRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'nota' => ['required', 'numeric', 'min:0', 'max:10'],
            'feedback' => ['nullable', 'string'],
        ];
    }
}
