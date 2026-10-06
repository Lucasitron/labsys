<?php

namespace App\Modules\Estoque\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Estoque\EstoquePrincipal;
use App\Modules\Estoque\Http\Requests\EntradaRequest;
use App\Modules\Estoque\Http\Resources\EntradaResource;
use App\Modules\Estoque\Services\EntradaService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Entradas — só HTTP. Filtro: só `?idItem=` (sem `?status=` — E-1). */
class EntradaController
{
    public function __construct(private EntradaService $entradas) {}

    public function store(EntradaRequest $request): JsonResponse
    {
        $entrada = $this->entradas->registrar($request->validated(), $this->principal($request));

        return response()->json((new EntradaResource($entrada))->toArray($request), 201);
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'idItem' => ['nullable', 'integer'],
        ]);

        $entradas = $this->entradas->listar(
            isset($filtros['idItem']) ? (int) $filtros['idItem'] : null,
            $this->principal($request),
        );

        return response()->json(array_map(
            fn ($e) => (new EntradaResource($e))->toArray($request),
            $entradas,
        ));
    }

    public function show(Request $request, int $id): EntradaResource
    {
        return new EntradaResource($this->entradas->buscar($id, $this->principal($request)));
    }

    private function principal(Request $request): EstoquePrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return EstoquePrincipal::from($login);
    }
}
