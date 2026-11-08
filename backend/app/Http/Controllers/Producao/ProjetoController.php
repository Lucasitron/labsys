<?php

namespace App\Http\Controllers\Producao;

use App\Enums\Producao\ProjetoStatus;
use App\Http\Controllers\Controller;
use App\Http\Requests\Producao\ProjetoRequest;
use App\Http\Requests\Producao\ProjetoStatusRequest;
use App\Http\Resources\Producao\ProjetoResource;
use App\Services\Producao\ProjetoService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Projetos — só HTTP (vínculos e carimbos no service). */
class ProjetoController extends Controller
{
    public function __construct(private ProjetoService $projetos) {}

    public function store(ProjetoRequest $request): JsonResponse
    {
        $projeto = $this->projetos->criar($request->validated(), $this->principal($request));

        return response()->json(new ProjetoResource($projeto), 201);
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'status' => ['nullable', 'string', 'in:PLANEJADO,EM_ANDAMENTO,CONCLUIDO,CANCELADO'],
            'idResponsavel' => ['nullable', 'integer'],
        ]);

        $projetos = $this->projetos->listar(
            isset($filtros['status']) ? ProjetoStatus::from($filtros['status']) : null,
            isset($filtros['idResponsavel']) ? (int) $filtros['idResponsavel'] : null,
            $this->principal($request),
        );

        return response()->json(ProjetoResource::collection($projetos));
    }

    public function show(Request $request, int $id): JsonResponse
    {
        return response()->json(new ProjetoResource($this->projetos->detalhar($id, $this->principal($request))));
    }

    public function update(ProjetoRequest $request, int $id): JsonResponse
    {
        $projeto = $this->projetos->atualizar($id, $request->validated(), $this->principal($request));

        return response()->json(new ProjetoResource($projeto));
    }

    public function alterarStatus(ProjetoStatusRequest $request, int $id): JsonResponse
    {
        $dados = $request->validated();

        $projeto = $this->projetos->alterarStatus(
            $id, ProjetoStatus::from($dados['status']), $dados['dataFimReal'] ?? null, $this->principal($request),
        );

        return response()->json(new ProjetoResource($projeto));
    }

    public function destroy(Request $request, int $id): JsonResponse
    {
        $this->projetos->remover($id, $this->principal($request));

        return response()->json(null, 204);
    }
}
