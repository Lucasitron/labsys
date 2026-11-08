<?php

namespace App\Http\Requests\Auth;

use Illuminate\Foundation\Http\FormRequest;

/** POST /api/auth/refresh — renova o par com rotação. */
class RefreshRequest extends FormRequest
{
    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'refreshToken' => ['required', 'string'],
        ];
    }
}
