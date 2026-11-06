<?php

namespace App\Http\Requests\Vendas;

use App\Rules\DocumentoRule;
use Illuminate\Foundation\Http\FormRequest;

/** Cliente PF/PJ (cadastro/atualização): documento com dígitos, unicidade no service. */
class ClienteRequest extends FormRequest
{
    /** @return array<string, mixed> */
    public function rules(): array
    {
        return [
            'tipoPessoa' => ['required', 'string', 'in:PF,PJ'],
            'nomeRazaoSocial' => ['required', 'string', 'max:255'],
            'cpfCnpj' => ['required', 'string', 'max:20', new DocumentoRule],
            'email' => ['nullable', 'email', 'max:255'],
            'telefone' => ['nullable', 'string', 'max:32'],
            'endereco' => ['nullable', 'string', 'max:500'],
        ];
    }
}
