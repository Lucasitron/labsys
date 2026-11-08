<?php

namespace App\Http\Requests\Vendas;

use Illuminate\Foundation\Http\FormRequest;

/** Vínculo de tag a cliente ({tagId} no corpo). */
class VinculoTagRequest extends FormRequest
{
    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'tagId' => ['required', 'integer'],
        ];
    }
}
