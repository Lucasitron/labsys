<?php

namespace App\Modules\Estoque\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Estoque\EstoquePrincipal;
use App\Modules\Estoque\Http\Requests\SaidaRequest;
use App\Modules\Estoque\Http\Resources\SaidaResource;
use App\Modules\Estoque\Services\SaidaService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Saídas — só HTTP. Filtro: só `?idItem=` (sem `?status=` — E-1). */
class SaidaController
{
    public function __construct(private SaidaService $saidas) {}

    public function store(SaidaRequest $request): JsonResponse
    {
        $saida = $this->saidas->registrar($request->validated(), $this->principal($request));

        return response()->json((new SaidaResource($saida))->toArray($request), 201);
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'idItem' => ['nullable', 'integer'],
        ]);

        $saidas = $this->saidas->listar(
            isset($filtros['idItem']) ? (int) $filtros['idItem'] : null,
            $this->principal($request),
        );

        return response()->json(array_map(
            fn ($s) => (new SaidaResource($s))->toArray($request),
            $saidas,
        ));
    }

    public function show(Request $request, int $id): SaidaResource
    {
        return new SaidaResource($this->saidas->buscar($id, $this->principal($request)));
    }

    private function principal(Request $request): EstoquePrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return EstoquePrincipal::from($login);
    }
}
