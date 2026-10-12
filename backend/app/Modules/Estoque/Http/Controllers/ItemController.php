<?php

namespace App\Modules\Estoque\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Estoque\Enums\Categoria;
use App\Modules\Estoque\EstoquePrincipal;
use App\Modules\Estoque\Http\Requests\ItemRequest;
use App\Modules\Estoque\Http\Resources\ItemResource;
use App\Modules\Estoque\Http\Resources\SaidaResource;
use App\Modules\Estoque\Services\ItemService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\StreamedResponse;

/** Itens + CSV — só HTTP: FormRequest valida, service decide, Resource dá o shape. */
class ItemController
{
    public function __construct(private ItemService $itens) {}

    public function store(ItemRequest $request): JsonResponse
    {
        $item = $this->itens->criar($request->validated(), $this->principal($request));

        return response()->json((new ItemResource($item))->toArray($request), 201);
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'categoria' => ['nullable', 'string', 'in:INSUMO,FERRAMENTA,PECA'],
            'idLocalizacao' => ['nullable', 'integer'],
            'baixo' => ['nullable', 'boolean'],
        ]);

        $itens = $this->itens->listar(
            isset($filtros['categoria']) ? Categoria::from($filtros['categoria']) : null,
            isset($filtros['idLocalizacao']) ? (int) $filtros['idLocalizacao'] : null,
            isset($filtros['baixo']) ? (bool) $filtros['baixo'] : null,
            $this->principal($request),
        );

        return response()->json(array_map(
            fn ($i) => (new ItemResource($i))->toArray($request),
            $itens,
        ));
    }

    public function show(Request $request, int $id): ItemResource
    {
        return new ItemResource($this->itens->buscar($id, $this->principal($request)));
    }

    public function update(ItemRequest $request, int $id): JsonResponse
    {
        $item = $this->itens->atualizar($id, $request->validated(), $this->principal($request));

        return response()->json((new ItemResource($item))->toArray($request));
    }

    public function export(Request $request): StreamedResponse
    {
        $csv = $this->itens->exportarCsv($this->principal($request));

        return response()->streamDownload(
            function () use ($csv): void {
                echo $csv;
            },
            'itens.csv',
            ['Content-Type' => 'text/csv; charset=UTF-8'],
        );
    }

    public function import(Request $request): JsonResponse
    {
        $arquivo = $request->validate([
            'arquivo' => ['required', 'file', 'max:5120'],
        ])['arquivo'];

        $importados = $this->itens->importarCsv(
            (string) file_get_contents($arquivo->getRealPath()),
            $this->principal($request),
        );

        return response()->json(array_map(
            fn ($i) => (new ItemResource($i))->toArray($request),
            $importados,
        ), 201);
    }

    private function principal(Request $request): EstoquePrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return EstoquePrincipal::from($login);
    }
}
