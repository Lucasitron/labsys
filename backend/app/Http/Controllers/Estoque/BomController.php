<?php

namespace App\Http\Controllers\Estoque;

use App\Http\Controllers\Controller;
use App\Http\Controllers\Estoque\ResolvesEstoquePrincipal;

use App\Http\Requests\Estoque\BomConsumoRequest;
use App\Http\Requests\Estoque\BomRequest;
use App\Http\Resources\Estoque\BomResource;
use App\Services\Estoque\BomService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** BOMs — só HTTP (`?projetoId=` filtra pelo produto/serviço). */
class BomController extends Controller
{
    use ResolvesEstoquePrincipal;
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
}
