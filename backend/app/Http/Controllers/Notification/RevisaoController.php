<?php

namespace App\Http\Controllers\Notification;

use App\Http\Controllers\Controller;

use App\Http\Requests\Notification\RevisarRequest;
use App\Shared\Auth\Caller;
use App\Services\Notification\NotificacaoService;
use Illuminate\Http\JsonResponse;

/** Revisão do Admin — só HTTP (move ativas p/ o histórico; D9/docs/07 §6). */
class RevisaoController extends Controller
{
    public function __construct(private NotificacaoService $inbox) {}

    public function store(RevisarRequest $request): JsonResponse
    {
        return response()->json([
            'revisadas' => $this->inbox->revisar(
                $request->ids(),
                Caller::from($request->user())->idUser,
            ),
        ]);
    }
}
