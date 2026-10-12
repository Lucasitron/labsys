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
            // Vínculo (RH) — opcional: cria funcionário com o nível/departamento.
            'nivelAcesso' => ['nullable', 'integer', 'min:0', 'max:4'],
            'departamento' => ['nullable', 'string', 'max:255'],
            // Acesso (Auth) — opcional: provisiona credencial para a pessoa.
            'criarAcesso' => ['nullable', 'boolean'],
            'email' => ['nullable', 'required_if:criarAcesso,true', 'email', 'max:255'],
            'senha' => ['nullable', 'string', 'min:8'],
            'nomeUsuario' => ['nullable', 'string', 'max:255'],
            'setor' => ['nullable', 'string', 'max:255'],
        ];
    }
}
