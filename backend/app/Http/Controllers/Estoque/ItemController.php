<?php

namespace App\Http\Controllers\Estoque;

use App\Http\Controllers\Controller;
use App\Http\Controllers\Estoque\Concerns\ResolvesEstoquePrincipal;

use App\Enums\Estoque\Categoria;
use App\Http\Requests\Estoque\ItemRequest;
use App\Http\Resources\Estoque\ItemResource;
use App\Services\Estoque\ItemService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\StreamedResponse;

/** Itens + CSV — só HTTP: FormRequest valida, service decide, Resource dá o shape. */
class ItemController extends Controller
{
    use ResolvesEstoquePrincipal;
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
}
