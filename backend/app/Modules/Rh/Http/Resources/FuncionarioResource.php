<?php

namespace App\Modules\Rh\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

class FuncionarioResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id,
            'idPessoa' => (int) $this->id_pessoa,
            'nomePessoa' => $this->pessoa->nome_completo,
            'nivelAcesso' => $this->nivel_acesso->name,
            'departamento' => $this->departamento,
        ];
    }
}
