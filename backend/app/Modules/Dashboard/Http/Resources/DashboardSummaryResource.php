<?php

namespace App\Modules\Dashboard\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * Shape do `GET /dashboard/summary` (keys EN 1:1 com o front
 * `frontend/src/lib/types/dashboard.ts`).
 *
 * @mixin array{tasks:array,kpis:array,ordersByStatus:array,machinesByStatus:array,activity:array}
 */
class DashboardSummaryResource extends JsonResource
{
    /** @param Request $request */
    public function toArray($request): array
    {
        $resumo = $this->resource;

        return [
            'tasks' => $resumo['tasks'],
            'kpis' => $resumo['kpis'],
            'ordersByStatus' => $resumo['ordersByStatus'],
            'machinesByStatus' => $resumo['machinesByStatus'],
            'activity' => $resumo['activity'],
        ];
    }
}
