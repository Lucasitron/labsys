<?php

namespace App\Modules\Auth\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** POST /api/auth/validate-rfid — UUID do cartão (ESP32). */
class ValidateRfidRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'uuid' => ['required', 'string', 'max:255'],
        ];
    }
}
