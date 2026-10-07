<?php

namespace App\Modules\Producao\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Producao\Enums\KanbanStatus;
use App\Modules\Producao\Http\Requests\KanbanIncluirRequest;
use App\Modules\Producao\Http\Requests\KanbanMoverRequest;
use App\Modules\Producao\Http\Resources\HistoricoKanbanResource;
use App\Modules\Producao\Http\Resources\KanbanResource;
use App\Modules\Producao\ProducaoPrincipal;
use App\Modules\Producao\Services\KanbanService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Kanban — só HTTP (LINKA Vendas, `version`, histórico e eventos no service). */
class KanbanController
{
    public function __construct(private KanbanService $kanban) {}

    public function store(KanbanIncluirRequest $request): JsonResponse
    {
        $cartao = $this->kanban->incluir($request->validated(), $this->principal($request));

        return response()->json(new KanbanResource($cartao), 201);
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'status' => ['nullable', 'string', 'in:FILA,PRODUCAO,ACABAMENTO,PRONTO,ENTREGUE'],
        ]);

        $cartoes = $this->kanban->listar(
            isset($filtros['status']) ? KanbanStatus::from($filtros['status']) : null,
            $this->principal($request),
        );

        return response()->json(KanbanResource::collection($cartoes));
    }

    public function porEncomenda(Request $request, int $idEncomenda): JsonResponse
    {
        $cartao = $this->kanban->buscarPorEncomenda($idEncomenda, $this->principal($request));

        return response()->json(new KanbanResource($cartao));
    }

    public function historico(Request $request, int $idEncomenda): JsonResponse
    {
        $trilha = $this->kanban->historico($idEncomenda, $this->principal($request));

        return response()->json(HistoricoKanbanResource::collection($trilha));
    }

    public function mover(KanbanMoverRequest $request, int $id): JsonResponse
    {
        $dados = $request->validated();

        $cartao = $this->kanban->mover(
            $id, KanbanStatus::from($dados['statusNovo']), (int) $dados['version'],
            $dados['observacao'] ?? null, $this->principal($request),
        );

        return response()->json(new KanbanResource($cartao));
    }

    public function destroy(Request $request, int $id): JsonResponse
    {
        $this->kanban->remover($id, $this->principal($request));

        return response()->json(null, 204);
    }

    private function principal(Request $request): ProducaoPrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return ProducaoPrincipal::from($login);
    }
}
