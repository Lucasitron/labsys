<?php

namespace App\Modules\Producao\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Item de checklist do setor. */
class ChecklistResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_checklist,
            'idSetor' => (int) $this->id_setor,
            'item' => $this->item,
            'ativo' => (bool) $this->ativo,
        ];
    }
}
