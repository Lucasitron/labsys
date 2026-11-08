<?php

namespace App\Http\Requests\Notification;

use Illuminate\Foundation\Http\FormRequest;

/** Atualização de configuração de canal (D9 — Admin). */
class CanalRequest extends FormRequest
{
    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'canal' => ['nullable', 'string', 'max:20'],
            'habilitado' => ['required', 'boolean'],
            'parametros' => ['nullable', 'array'],
        ];
    }
}
