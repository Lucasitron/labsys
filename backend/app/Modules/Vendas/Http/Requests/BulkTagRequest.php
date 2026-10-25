<?php

namespace App\Modules\Vendas\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** Marcação de tag em lote (Admin-only). */
class BulkTagRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'clienteIds' => ['required', 'array', 'min:1'],
            'clienteIds.*' => ['integer'],
            'tagId' => ['required', 'integer'],
        ];
    }
}
