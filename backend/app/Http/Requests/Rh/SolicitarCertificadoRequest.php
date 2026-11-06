<?php

namespace App\Http\Requests\Rh;

use Illuminate\Foundation\Http\FormRequest;

/** POST certificados/solicitar — horas ≤ disponíveis (checadas no service). */
class SolicitarCertificadoRequest extends FormRequest
{
    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'tipoCertificado' => ['required', 'in:EXTENSAO,COMPLEMENTAR,ESTAGIO'],
            'horasSolicitadas' => ['required', 'numeric', 'min:0.01', 'max:9999.99'],
        ];
    }
}
