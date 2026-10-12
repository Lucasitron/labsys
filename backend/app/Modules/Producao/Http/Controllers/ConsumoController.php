<?php

namespace App\Modules\Producao\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Producao\Http\Requests\ConsumoRequest;
use App\Modules\Producao\Http\Resources\ConsumoResource;
use App\Modules\Producao\ProducaoPrincipal;
use App\Modules\Producao\Services\KanbanService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** BOM final por encomenda — só HTTP (upsert idempotente no `KanbanService`). */
class ConsumoController
{
    public function __construct(private KanbanService $kanban) {}

    public function store(ConsumoRequest $request, int $idEncomenda): JsonResponse
    {
        $itens = $this->kanban->registrarConsumo(
            $idEncomenda, $request->validated()['itens'], $this->principal($request),
        );

        return response()->json(ConsumoResource::collection($itens), 201);
    }

    public function index(Request $request, int $idEncomenda): JsonResponse
    {
        $itens = $this->kanban->listarConsumo($idEncomenda, $this->principal($request));

        return response()->json(ConsumoResource::collection($itens));
    }

    private function principal(Request $request): ProducaoPrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return ProducaoPrincipal::from($login);
    }
}
