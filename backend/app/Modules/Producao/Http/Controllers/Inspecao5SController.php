<?php

namespace App\Modules\Producao\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Producao\Http\Requests\InspecaoRequest;
use App\Modules\Producao\Http\Resources\InspecaoResource;
use App\Modules\Producao\ProducaoPrincipal;
use App\Modules\Producao\Services\Inspecao5SService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Inspeções 5S — só HTTP (status recalculado e auto-advertência no service). */
class Inspecao5SController
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

    private function principal(Request $request): ProducaoPrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return ProducaoPrincipal::from($login);
    }
}
