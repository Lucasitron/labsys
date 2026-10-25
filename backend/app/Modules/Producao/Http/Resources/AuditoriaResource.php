<?php

namespace App\Modules\Producao\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Auditoria de projeto de mesa. */
class AuditoriaResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_auditoria,
            'idProjetoMesa' => (int) $this->id_projeto_mesa,
            'dataAuditoria' => substr((string) $this->data_auditoria, 0, 10),
            'resultado' => $this->resultado->value,
            'acaoTomada' => $this->acao_tomada,
            'idAdminResponsavel' => (int) $this->id_admin_responsavel,
        ];
    }
}
