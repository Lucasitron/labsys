<?php

namespace App\Modules\Vendas\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** Vínculo de tag a cliente ({tagId} no corpo). */
class VinculoTagRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'tagId' => ['required', 'integer'],
        ];
    }
}
