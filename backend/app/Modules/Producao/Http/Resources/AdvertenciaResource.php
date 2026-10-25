<?php

namespace App\Modules\Producao\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Advertência de membro. */
class AdvertenciaResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_advertencia,
            'idFuncionario' => (int) $this->id_funcionario,
            'idInspecao' => $this->id_inspecao === null ? null : (int) $this->id_inspecao,
            'data' => substr((string) $this->data, 0, 10),
            'motivo' => $this->motivo,
            'tipo' => $this->tipo->value,
            'contador' => (int) $this->contador,
            'idAdminRegistrou' => $this->id_admin_registrou === null ? null : (int) $this->id_admin_registrou,
        ];
    }
}
