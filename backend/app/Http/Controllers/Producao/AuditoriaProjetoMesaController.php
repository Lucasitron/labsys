<?php

namespace App\Http\Controllers\Producao;

use App\Http\Controllers\Controller;
use App\Http\Requests\Producao\AuditoriaRequest;
use App\Http\Resources\Producao\AuditoriaResource;
use App\Services\Producao\ProjetoMesaService;
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
