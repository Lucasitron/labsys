<?php

namespace App\Modules\Notification\Http\Requests;

use App\Modules\Auth\Models\Login;
use Illuminate\Foundation\Http\FormRequest;

/** Revisão do Admin: ids das ativas a mover p/ o histórico (D9). */
class RevisarRequest extends FormRequest
{
    public function authorize(): bool
    {
        return $this->user() instanceof Login;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'ids' => ['required', 'array', 'min:1'],
            'ids.*' => ['integer', 'min:1'],
        ];
    }

    /** @return list<int> */
    public function ids(): array
    {
        return array_map('intval', (array) $this->input('ids', []));
    }
}
