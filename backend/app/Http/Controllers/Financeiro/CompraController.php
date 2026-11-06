<?php

namespace App\Http\Controllers\Financeiro;

use App\Http\Controllers\Controller;

use App\Models\Auth\Login;
use App\Enums\Financeiro\StatusCompra;
use App\Shared\Auth\Caller;
use App\Http\Requests\Financeiro\CompraRequest;
use App\Http\Resources\Financeiro\CompraResource;
use App\Services\Financeiro\CompraService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Solicitações de compra — só HTTP. Sem rota de visualizar. */
class CompraController extends Controller
{
    public function __construct(private CompraService $compras) {}

    public function store(CompraRequest $request): JsonResponse
    {
        $compra = $this->compras->registrar($request->validated(), $this->principal($request));

        return response()->json((new CompraResource($compra))->toArray($request), 201);
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'status' => ['nullable', 'string', 'in:REGISTRADA,VISUALIZADA,CONCLUIDA'],
        ]);

        $compras = $this->compras->listar(
            isset($filtros['status']) ? StatusCompra::from($filtros['status']) : null,
            $this->principal($request),
        );

        return response()->json(array_map(
            fn ($c) => (new CompraResource($c))->toArray($request),
            $compras,
        ));
    }

    public function show(Request $request, int $id): JsonResponse
    {
        $compra = $this->compras->detalhar($id, $this->principal($request));

        return response()->json((new CompraResource($compra))->toArray($request));
    }

    public function concluir(Request $request, int $id): JsonResponse
    {
        $compra = $this->compras->concluir($id, $this->principal($request));

        return response()->json((new CompraResource($compra))->toArray($request));
    }

}
