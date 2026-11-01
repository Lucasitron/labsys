<?php

namespace App\Modules\Notification\Http\Controllers;

use App\Http\Controllers\Controller;

use App\Modules\Notification\Http\Requests\RevisarRequest;
use App\Shared\Auth\Caller;
use App\Modules\Notification\Services\NotificacaoService;
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
