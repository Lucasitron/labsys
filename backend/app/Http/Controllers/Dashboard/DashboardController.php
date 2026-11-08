<?php

namespace App\Http\Controllers\Dashboard;

use App\Contracts\AuthContract;
use App\Http\Controllers\Controller;
use App\Http\Resources\Dashboard\DashboardSummaryResource;
use App\Models\Auth\Login;
use App\Services\Dashboard\DashboardService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Dashboard — só HTTP (agregador sem persistência; sem FormRequest: GET sem input). */
class DashboardController extends Controller
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
