<?php

namespace App\Modules\Auth\Http\Controllers;

use App\Modules\Auth\Http\Requests\AtualizarSistemaRequest;
use App\Modules\Auth\Services\SistemaService;
use Illuminate\Http\JsonResponse;

/** Parâmetros globais do sistema (Admin-only) — só HTTP. */
class ConfiguracaoSistemaController
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
