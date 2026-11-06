<?php

namespace App\Http\Controllers\Producao;

use App\Http\Controllers\Controller;

use App\Models\Auth\Login;
use App\Http\Requests\Producao\InspecaoRequest;
use App\Http\Resources\Producao\InspecaoResource;
use App\Shared\Auth\Caller;
use App\Services\Producao\Inspecao5SService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Inspeções 5S — só HTTP (status recalculado e auto-advertência no service). */
class Inspecao5SController extends Controller
{
    public function __construct(private Inspecao5SService $inspecoes) {}

    public function store(InspecaoRequest $request): JsonResponse
    {
        $inspecao = $this->inspecoes->registrar($request->validated(), $this->principal($request));

        return response()->json(new InspecaoResource($inspecao), 201);
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'idSetor' => ['nullable', 'integer'],
        ]);

        $inspecoes = $this->inspecoes->listar(
            isset($filtros['idSetor']) ? (int) $filtros['idSetor'] : null,
            $this->principal($request),
        );

        return response()->json(InspecaoResource::collection($inspecoes));
    }

    public function show(Request $request, int $id): JsonResponse
    {
        return response()->json(new InspecaoResource($this->inspecoes->detalhar($id, $this->principal($request))));
    }

    public function destroy(Request $request, int $id): JsonResponse
    {
        $this->inspecoes->remover($id, $this->principal($request));

        return response()->json(null, 204);
    }

}
