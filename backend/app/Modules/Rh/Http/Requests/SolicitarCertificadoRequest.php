<?php

namespace App\Modules\Rh\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** POST certificados/solicitar — horas ≤ disponíveis (checadas no service). */
class SolicitarCertificadoRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'tipoCertificado' => ['required', 'in:EXTENSAO,COMPLEMENTAR,ESTAGIO'],
            'horasSolicitadas' => ['required', 'numeric', 'min:0.01', 'max:9999.99'],
        ];
    }
}
