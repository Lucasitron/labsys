<?php

namespace App\Modules\Rh\Http\Requests;

use App\Modules\Rh\Rules\CpfRule;
use Illuminate\Foundation\Http\FormRequest;

/** Pessoa (cadastro/atualização): matrícula única e CPF opcional checados no service. */
class PessoaRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'nomeCompleto' => ['required', 'string', 'max:255'],
            'matricula' => ['required', 'string', 'max:64'],
            'dataAdmissao' => ['nullable', 'date'],
            'contato' => ['nullable', 'string', 'max:255'],
            'turno' => ['nullable', 'string', 'max:32'],
            'status' => ['nullable', 'integer', 'min:0', 'max:2'],
            'cpf' => ['nullable', 'string', 'max:18', new CpfRule],
        ];
    }
}
