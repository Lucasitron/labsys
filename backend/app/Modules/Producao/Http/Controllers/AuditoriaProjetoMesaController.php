<?php

namespace App\Modules\Producao\Http\Controllers;

use App\Http\Controllers\Controller;

use App\Models\Auth\Login;
use App\Modules\Producao\Http\Requests\AuditoriaRequest;
use App\Modules\Producao\Http\Resources\AuditoriaResource;
use App\Shared\Auth\Caller;
use App\Modules\Producao\Services\ProjetoMesaService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Auditorias de projeto de mesa — só HTTP (auditar é Admin, dobrado no `ProjetoMesaService`). */
class AuditoriaProjetoMesaController extends Controller
{
    public function __construct(private ProjetoMesaService $mesas) {}

    public function store(AuditoriaRequest $request): JsonResponse
    {
        $auditoria = $this->mesas->auditar($request->validated(), $this->principal($request));

        return response()->json(new AuditoriaResource($auditoria), 201);
    }

    public function porMesa(Request $request, int $idProjetoMesa): JsonResponse
    {
        $lista = $this->mesas->listarAuditorias($idProjetoMesa, $this->principal($request));

        return response()->json(AuditoriaResource::collection($lista));
    }

}
