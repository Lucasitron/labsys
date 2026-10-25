<?php

namespace App\Modules\Financeiro\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Financeiro\Enums\StatusCompra;
use App\Modules\Financeiro\FinanceiroPrincipal;
use App\Modules\Financeiro\Http\Requests\CompraRequest;
use App\Modules\Financeiro\Http\Resources\CompraResource;
use App\Modules\Financeiro\Services\CompraService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Solicitações de compra — só HTTP. Sem rota de visualizar. */
class CompraController
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

    private function principal(Request $request): FinanceiroPrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return FinanceiroPrincipal::from($login);
    }
}
