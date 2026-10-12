<?php

namespace App\Modules\Vendas\Http\Resources;

use App\Modules\Vendas\Rules\DocumentoRule;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Cliente com documento mascarado (LGPD) e tags. */
class ClienteResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_cliente,
            'tipoPessoa' => $this->tipo_pessoa->value,
            'nomeRazaoSocial' => $this->nome_razao_social,
            'documento' => DocumentoRule::mascarar((string) $this->cpf_cnpj),
            'email' => $this->email,
            'telefone' => $this->telefone,
            'endereco' => $this->endereco,
            'dataCadastro' => substr((string) $this->data_cadastro, 0, 10),
            'tags' => $this->relationLoaded('tags')
                ? TagResource::collection($this->tags)->toArray($request)
                : [],
            'criadoPor' => (int) $this->criado_por,
        ];
    }
}
