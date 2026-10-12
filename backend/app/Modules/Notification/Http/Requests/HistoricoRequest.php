<?php

namespace App\Modules\Notification\Http\Requests;

use App\Modules\Auth\Models\Login;
use Illuminate\Foundation\Http\FormRequest;

/** Filtros do histórico Admin (lista integral; `periodo` validado no service → 400). */
class HistoricoRequest extends FormRequest
{
    public function authorize(): bool
    {
        return $this->user() instanceof Login;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'search' => ['nullable', 'string', 'max:160'],
            'type' => ['nullable', 'string', 'max:40'],
            'read' => ['nullable'],
            'channel' => ['nullable', 'string', 'max:20'],
            'period' => ['nullable', 'string', 'max:10'],
        ];
    }

    public function lida(): ?bool
    {
        if (! $this->has('read')) {
            return null;
        }

        return filter_var($this->input('read'), FILTER_VALIDATE_BOOLEAN, FILTER_NULL_ON_FAILURE);
    }
}
