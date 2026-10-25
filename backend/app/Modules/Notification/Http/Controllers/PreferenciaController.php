<?php

namespace App\Modules\Notification\Http\Controllers;

use App\Modules\Notification\Http\Requests\AtualizarPreferenciasRequest;
use App\Modules\Notification\Services\NotificacaoService;
use Illuminate\Http\JsonResponse;

/** Preferências tipo×canal — só HTTP (Admin; validação estrita no service → 400). */
class PreferenciaController
{
    public function __construct(private NotificacaoService $inbox) {}

    public function index(): JsonResponse
    {
        return response()->json($this->inbox->obterPreferencias());
    }

    public function update(AtualizarPreferenciasRequest $request): JsonResponse
    {
        return response()->json($this->inbox->salvarPreferencias($request->matriz()));
    }
}
