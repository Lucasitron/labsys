<?php

namespace App\Modules\Auth\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** POST /api/auth/refresh — renova o par com rotação. */
class RefreshRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'refreshToken' => ['required', 'string'],
        ];
    }
}
