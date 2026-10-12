<?php

namespace App\Modules\Rh\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/** Apontamento: com horaInicio+hFim o servidor recalcula (ignora o enviado). */
class RegistrarApontamentoRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'idFuncionario' => ['required', 'integer'],
            'tipo' => ['required', 'in:ENCOMENDA,PROJETO'],
            'idReferencia' => ['required', 'integer'],
            'data' => ['required', 'date'],
            'horasTrabalhadas' => ['nullable', 'numeric', 'min:0.01', 'max:99.99'],
            'horaInicio' => ['nullable', 'date_format:H:i'],
            'horaFim' => ['nullable', 'date_format:H:i'],
            'descricaoAtividade' => ['nullable', 'string'],
        ];
    }
}
