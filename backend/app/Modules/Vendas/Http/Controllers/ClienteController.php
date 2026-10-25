<?php

namespace App\Modules\Vendas\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Vendas\Enums\TipoPessoa;
use App\Modules\Vendas\Http\Requests\BulkTagRequest;
use App\Modules\Vendas\Http\Requests\ClienteRequest;
use App\Modules\Vendas\Http\Requests\VinculoTagRequest;
use App\Modules\Vendas\Http\Resources\ClienteResource;
use App\Modules\Vendas\Services\ClienteService;
use App\Modules\Vendas\VendasPrincipal;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Clientes PF/PJ — só HTTP: FormRequest valida, service decide, Resource dá o shape. */
class ClienteController
{
    public function __construct(private ClienteService $clientes) {}

    public function store(ClienteRequest $request): JsonResponse
    {
        $cliente = $this->clientes->criar($request->validated(), $this->principal($request));

        return response()->json((new ClienteResource($cliente->load('tags')))->toArray($request), 201);
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'termo' => ['nullable', 'string', 'max:255'],
            'tipo' => ['nullable', 'string', 'in:PF,PJ'],
            'tags' => ['nullable'],
            'tags.*' => ['integer'],
            'page' => ['nullable', 'integer', 'min:1'],
            'pageSize' => ['nullable', 'integer', 'min:1', 'max:100'],
        ]);

        $pagina = $this->clientes->listar(
            $filtros['termo'] ?? null,
            isset($filtros['tipo']) ? TipoPessoa::from($filtros['tipo']) : null,
            $this->tagIds($filtros['tags'] ?? null),
            (int) ($filtros['page'] ?? 1),
            (int) ($filtros['pageSize'] ?? 10),
            $this->principal($request),
        );

        return response()->json([
            'clientes' => $pagina->getCollection()->map(
                fn ($c) => (new ClienteResource($c))->toArray($request)
            )->all(),
            'paginacao' => [
                'page' => $pagina->currentPage(), 'pageSize' => $pagina->perPage(),
                'total' => $pagina->total(), 'totalPages' => $pagina->lastPage(),
            ],
        ]);
    }

    public function show(Request $request, int $id): JsonResponse
    {
        $dados = $this->clientes->detalhar($id, $this->principal($request));

        return response()->json([
            'cliente' => (new ClienteResource($dados['cliente']))->toArray($request),
            'indicadores' => $dados['indicadores'],
        ]);
    }

    public function update(ClienteRequest $request, int $id): JsonResponse
    {
        $cliente = $this->clientes->atualizar($id, $request->validated(), $this->principal($request));

        return response()->json((new ClienteResource($cliente))->toArray($request));
    }

    public function destroy(Request $request, int $id): JsonResponse
    {
        $this->clientes->excluir($id, $this->principal($request));

        return response()->json(null, 204);
    }

    public function vincularTag(VinculoTagRequest $request, int $id): JsonResponse
    {
        $this->clientes->vincularTag($id, (int) $request->validated()['tagId'], $this->principal($request));

        return response()->json(null, 201);
    }

    public function desvincularTag(Request $request, int $id, int $tagId): JsonResponse
    {
        $this->clientes->desvincularTag($id, $tagId, $this->principal($request));

        return response()->json(null, 204);
    }

    public function bulkTag(BulkTagRequest $request): JsonResponse
    {
        $dados = $request->validated();

        return response()->json($this->clientes->bulkTag(
            array_map(intval(...), $dados['clienteIds']),
            (int) $dados['tagId'],
            $this->principal($request),
        ));
    }

    /** @return list<int> */
    private function tagIds(mixed $tags): array
    {
        if ($tags === null || $tags === '') {
            return [];
        }
        if (is_string($tags)) {
            $tags = explode(',', $tags);
        }

        return array_values(array_filter(array_map(
            fn ($t) => is_numeric($t) ? (int) $t : null,
            (array) $tags,
        ), fn ($t) => $t !== null));
    }

    private function principal(Request $request): VendasPrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return VendasPrincipal::from($login);
    }
}
