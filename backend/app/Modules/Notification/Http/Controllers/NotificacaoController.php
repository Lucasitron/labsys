<?php

namespace App\Modules\Notification\Http\Controllers;

use App\Modules\Notification\Http\Requests\ListarNotificacoesRequest;
use App\Modules\Notification\Http\Resources\NotificationResource;
use App\Modules\Notification\NotificationPrincipal;
use App\Modules\Notification\Services\NotificacaoService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Inbox — só HTTP (qualquer autenticado, incl. Recrutando; isolamento no service). */
class NotificacaoController
{
    public function __construct(private NotificacaoService $inbox) {}

    public function index(ListarNotificacoesRequest $request): JsonResponse
    {
        $pagina = $this->inbox->listar(
            $this->principal($request)->idUser,
            $request->pagina(),
            $request->tamanho(),
            $request->input('type'),
            $request->lida(),
        );

        return response()->json([
            'items' => NotificationResource::collection($pagina['items']),
            'total' => $pagina['total'],
            'page' => $pagina['page'],
            'size' => $pagina['size'],
            'pageSize' => $pagina['pageSize'],
            'totalPages' => $pagina['totalPages'],
        ]);
    }

    public function naoLidas(Request $request): JsonResponse
    {
        return response()->json([
            'count' => $this->inbox->contarNaoLidas($this->principal($request)->idUser),
        ]);
    }

    public function ler(Request $request, int $id): JsonResponse
    {
        $principal = $this->principal($request);

        return response()->json(new NotificationResource(
            $this->inbox->marcarComoLida($id, $principal->idUser, $principal->isAdmin())
        ));
    }

    public function lerTodas(Request $request): JsonResponse
    {
        return response()->json([
            'lidas' => $this->inbox->marcarTodasComoLidas($this->principal($request)->idUser),
        ]);
    }

    private function principal(Request $request): NotificationPrincipal
    {
        return NotificationPrincipal::from($request->user());
    }
}
