<?php

namespace App\Modules\Producao\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Producao\Http\Requests\AuditoriaRequest;
use App\Modules\Producao\Http\Resources\AuditoriaResource;
use App\Modules\Producao\ProducaoPrincipal;
use App\Modules\Producao\Services\ProjetoMesaService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Auditorias de projeto de mesa — só HTTP (auditar é Admin, dobrado no `ProjetoMesaService`). */
class AuditoriaProjetoMesaController
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

    private function principal(Request $request): ProducaoPrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return ProducaoPrincipal::from($login);
    }
}
