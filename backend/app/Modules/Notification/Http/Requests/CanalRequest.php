<?php

namespace App\Modules\Notification\Http\Requests;

use App\Modules\Auth\Models\Login;
use Illuminate\Foundation\Http\FormRequest;

/** Atualização de configuração de canal (D9 — Admin). */
class CanalRequest extends FormRequest
{
    public function authorize(): bool
    {
        return $this->user() instanceof Login;
    }

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
