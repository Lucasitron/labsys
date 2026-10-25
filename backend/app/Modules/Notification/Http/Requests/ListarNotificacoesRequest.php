<?php

namespace App\Modules\Notification\Http\Requests;

use App\Modules\Auth\Models\Login;
use Illuminate\Foundation\Http\FormRequest;

/**
 * Lista paginada do inbox (faixas `page`/`size` validadas no service → 400;
 * aqui só tipos — rejeição de lixo antes do service).
 */
class ListarNotificacoesRequest extends FormRequest
{
    public function authorize(): bool
    {
        return $this->user() instanceof Login;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'page' => ['nullable', 'integer'],
            'size' => ['nullable', 'integer'],
            'pageSize' => ['nullable', 'integer'],
            'type' => ['nullable', 'string', 'max:40'],
            'read' => ['nullable'],
        ];
    }

    public function pagina(): int
    {
        return (int) ($this->input('page') ?? 1);
    }

    public function tamanho(): int
    {
        return (int) ($this->input('size') ?? $this->input('pageSize') ?? 10);
    }

    public function lida(): ?bool
    {
        if (! $this->has('read')) {
            return null;
        }

        return filter_var($this->input('read'), FILTER_VALIDATE_BOOLEAN, FILTER_NULL_ON_FAILURE);
    }
}
