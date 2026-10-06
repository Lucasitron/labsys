<?php

namespace App\Modules\Rh\Http\Resources;

use App\Modules\Rh\Rules\CpfRule;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Pessoa — CPF sempre mascarado (LGPD), nunca em claro. */
class PessoaResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id,
            'nomeCompleto' => $this->nome_completo,
            'matricula' => $this->matricula,
            'dataAdmissao' => $this->data_admissao?->toDateString(),
            'contato' => $this->contato,
            'turno' => $this->turno,
            'status' => $this->status->name,
            'cpf' => CpfRule::mascarar($this->cpf),
        ];
    }
}
