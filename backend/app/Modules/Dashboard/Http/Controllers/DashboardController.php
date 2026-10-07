<?php

namespace App\Modules\Dashboard\Http\Controllers;

use App\Modules\Auth\Contracts\AuthContract;
use App\Modules\Auth\Models\Login;
use App\Modules\Dashboard\Http\Resources\DashboardSummaryResource;
use App\Modules\Dashboard\Services\DashboardService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Dashboard — só HTTP (agregador sem persistência; sem FormRequest: GET sem input). */
class DashboardController
{
    public function __construct(private DashboardService $resumo) {}

    public function summary(Request $request): JsonResponse
    {
        /** @var Login $login */
        $login = $request->user();
        $idUsuario = (int) $login->id_user;

        return response()->json(new DashboardSummaryResource(
            $this->resumo->resumo($idUsuario, app(AuthContract::class)->roleOf($idUsuario))
        ));
    }
}
