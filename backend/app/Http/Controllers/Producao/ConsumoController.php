<?php

namespace App\Http\Controllers\Producao;

use App\Http\Controllers\Controller;
use App\Http\Requests\Producao\ConsumoRequest;
use App\Http\Resources\Producao\ConsumoResource;
use App\Services\Producao\KanbanService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** BOM final por encomenda — só HTTP (upsert idempotente no `KanbanService`). */
class ConsumoController extends Controller
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
}
