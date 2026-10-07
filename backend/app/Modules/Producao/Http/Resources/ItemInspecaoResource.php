<?php

namespace App\Modules\Producao\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Item avaliado na inspeção. */
class ItemInspecaoResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_item_inspecao,
            'idChecklist' => (int) $this->id_checklist,
            'conforme' => (bool) $this->conforme,
            'observacao' => $this->observacao,
        ];
    }
}
