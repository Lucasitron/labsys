<?php

namespace App\Http\Controllers\Notification;

use App\Http\Controllers\Controller;

use App\Http\Requests\Notification\AtualizarPreferenciasRequest;
use App\Services\Notification\NotificacaoService;
use Illuminate\Http\JsonResponse;

/** Preferências tipo×canal — só HTTP (Admin; validação estrita no service → 400). */
class PreferenciaController extends Controller
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
