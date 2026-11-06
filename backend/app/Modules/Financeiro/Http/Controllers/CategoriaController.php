<?php

namespace App\Modules\Financeiro\Http\Controllers;

use App\Http\Controllers\Controller;

use App\Models\Auth\Login;
use App\Shared\Auth\Caller;
use App\Modules\Financeiro\Http\Requests\CategoriaRequest;
use App\Modules\Financeiro\Http\Resources\CategoriaResource;
use App\Modules\Financeiro\Services\CategoriaService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Categorias financeiras — só HTTP (regras no service). */
class CategoriaController extends Controller
{
    public function __construct(private CategoriaService $categorias) {}

    public function store(CategoriaRequest $request): JsonResponse
    {
        $categoria = $this->categorias->criar($request->validated(), $this->principal($request));

        return response()->json((new CategoriaResource($categoria))->toArray($request), 201);
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate(['tipo' => ['nullable', 'string', 'in:RECEITA,DESPESA']]);

        // O service espera enum; a validação acima já garante o allowlist.
        $categorias = $this->categorias->listar(
            isset($filtros['tipo'])
                ? \App\Modules\Financeiro\Enums\TipoCategoria::from($filtros['tipo']) : null,
            $this->principal($request),
        );

        return response()->json(array_map(
            fn ($c) => (new CategoriaResource($c))->toArray($request),
            $categorias,
        ));
    }

}
