<?php

namespace App\Modules\Notification\Http\Controllers;

use App\Modules\Notification\Http\Requests\RevisarRequest;
use App\Modules\Notification\NotificationPrincipal;
use App\Modules\Notification\Services\NotificacaoService;
use Illuminate\Http\JsonResponse;

/** Revisão do Admin — só HTTP (move ativas p/ o histórico; D9/docs/07 §6). */
class RevisaoController
{
    public function __construct(private NotificacaoService $inbox) {}

    public function store(RevisarRequest $request): JsonResponse
    {
        return response()->json([
            'revisadas' => $this->inbox->revisar(
                $request->ids(),
                NotificationPrincipal::from($request->user())->idUser,
            ),
        ]);
    }
}
