<?php

namespace App\Http\Requests\Notification;

use Illuminate\Foundation\Http\FormRequest;

/** Filtros do histórico Admin (lista integral; `periodo` validado no service → 400). */
class HistoricoRequest extends FormRequest
{
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

        return $this->boolean('read');
    }
}
