<?php

namespace App\Modules\Notification\Http\Controllers;

use App\Modules\Notification\Http\Requests\HistoricoRequest;
use App\Modules\Notification\Http\Resources\NotificationResource;
use App\Modules\Notification\Services\NotificacaoService;
use Illuminate\Http\JsonResponse;

/** Histórico Admin — só HTTP (lista integral; front pagina client-side). */
class HistoricoController
{
    public function __construct(private NotificacaoService $inbox) {}

    public function index(HistoricoRequest $request): JsonResponse
    {
        $historico = $this->inbox->historico(
            $request->input('search'),
            $request->input('type'),
            $request->lida(),
            $request->input('channel'),
            $request->input('period'),
        );

        return response()->json([
            'items' => NotificationResource::collection($historico['items']),
            'total' => $historico['total'],
        ]);
    }
}
