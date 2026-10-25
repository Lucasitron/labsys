<?php

namespace App\Modules\Rh\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** POST /api/rh/treinamentos — Admin informa idTutor; tutor vira o próprio. */
class CriarTreinamentoRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'titulo' => ['required', 'string', 'max:255'],
            'descricao' => ['nullable', 'string'],
            'urlConteudo' => ['nullable', 'string', 'max:500'],
            'idTutor' => ['nullable', 'integer'],
        ];
    }
}
