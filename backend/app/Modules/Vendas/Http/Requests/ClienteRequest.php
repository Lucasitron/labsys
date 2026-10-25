<?php

namespace App\Modules\Vendas\Http\Requests;

use App\Modules\Vendas\Rules\DocumentoRule;
use Illuminate\Foundation\Http\FormRequest;

/** Cliente PF/PJ (cadastro/atualização): documento com dígitos, unicidade no service. */
class ClienteRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

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
