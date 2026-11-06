<?php

namespace App\Http\Controllers\Auth;

use App\Http\Controllers\Controller;

use App\Http\Requests\Auth\AtualizarSistemaRequest;
use App\Services\Auth\SistemaService;
use Illuminate\Http\JsonResponse;

/** Parâmetros globais do sistema (Admin-only) — só HTTP. */
class ConfiguracaoSistemaController extends Controller
{
    public function __construct(private SistemaService $sistema) {}

    public function show(): JsonResponse
    {
        return response()->json($this->sistema->obter());
    }

    public function update(AtualizarSistemaRequest $request): JsonResponse
    {
        return response()->json($this->sistema->atualizar($request->only([
            'identidade', 'cadenciaChecklist5S', 'cadenciaAuditoria5S',
        ])));
    }
}
