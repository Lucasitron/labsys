<?php

namespace App\Modules\Estoque\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Estoque\EstoquePrincipal;
use App\Modules\Estoque\Http\Requests\BomConsumoRequest;
use App\Modules\Estoque\Http\Requests\BomRequest;
use App\Modules\Estoque\Http\Resources\BomResource;
use App\Modules\Estoque\Services\BomService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** BOMs — só HTTP (`?projetoId=` filtra pelo produto/serviço). */
class BomController
{
    public function __construct(private BomService $boms) {}

    public function store(BomRequest $request): JsonResponse
    {
        $bom = $this->boms->criar($request->validated(), $this->principal($request));

        return response()->json((new BomResource($bom))->toArray($request), 201);
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'projetoId' => ['nullable', 'integer'],
        ]);

        $boms = $this->boms->listar(
            isset($filtros['projetoId']) ? (int) $filtros['projetoId'] : null,
            $this->principal($request),
        );

        return response()->json(array_map(
            fn ($b) => (new BomResource($b))->toArray($request),
            $boms,
        ));
    }

    public function show(Request $request, int $id): BomResource
    {
        return new BomResource($this->boms->buscar($id, $this->principal($request)));
    }

    public function update(BomRequest $request, int $id): JsonResponse
    {
        $bom = $this->boms->atualizar($id, $request->validated(), $this->principal($request));

        return response()->json((new BomResource($bom))->toArray($request));
    }

    public function consumo(BomConsumoRequest $request, int $id): JsonResponse
    {
        $bom = $this->boms->registrarConsumo($id, $request->validated()['itens'], $this->principal($request));

        return response()->json((new BomResource($bom))->toArray($request));
    }

    private function principal(Request $request): EstoquePrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return EstoquePrincipal::from($login);
    }
}
