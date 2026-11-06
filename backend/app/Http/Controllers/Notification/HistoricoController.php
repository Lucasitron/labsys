<?php

namespace App\Http\Controllers\Notification;

use App\Http\Controllers\Controller;

use App\Http\Requests\Notification\HistoricoRequest;
use App\Http\Resources\Notification\NotificationResource;
use App\Services\Notification\NotificacaoService;
use Illuminate\Http\JsonResponse;

/** Histórico Admin — só HTTP (lista integral; front pagina client-side). */
class HistoricoController extends Controller
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
            'items' => NotificationResource::collection($historico['items'])->toArray($request),
            'total' => $historico['total'],
        ]);
    }
}
