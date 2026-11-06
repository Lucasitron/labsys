<?php

namespace App\Http\Controllers\Notification;

use App\Http\Controllers\Controller;

use App\Http\Requests\Notification\ListarNotificacoesRequest;
use App\Http\Resources\Notification\NotificationResource;
use App\Shared\Auth\Caller;
use App\Services\Notification\NotificacaoService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Inbox — só HTTP (qualquer autenticado, incl. Recrutando; isolamento no service). */
class NotificacaoController extends Controller
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
            'items' => NotificationResource::collection($pagina['items'])->toArray($request),
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

}
