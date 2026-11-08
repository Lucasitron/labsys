<?php

namespace App\Http\Controllers\Producao;

use App\Enums\Producao\TarefaStatus;
use App\Http\Controllers\Controller;
use App\Http\Requests\Producao\TarefaRequest;
use App\Http\Requests\Producao\TarefaStatusRequest;
use App\Http\Resources\Producao\TarefaResource;
use App\Services\Producao\ProjetoService;
use App\Services\Producao\TarefaService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Tarefas — só HTTP (`PATCH` conclui a própria tarefa, compat M8). */
class TarefaController extends Controller
{
    public function __construct(private TarefaService $tarefas, private ProjetoService $projetos) {}

    public function store(TarefaRequest $request): JsonResponse
    {
        $tarefa = $this->tarefas->criar($request->validated(), $this->principal($request), $this->projetos);

        return response()->json(new TarefaResource($tarefa), 201);
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'idProjeto' => ['nullable', 'integer'],
            'idResponsavel' => ['nullable', 'integer'],
            'status' => ['nullable', 'string', 'in:PENDENTE,EM_ANDAMENTO,CONCLUIDA,ATRASADA'],
        ]);

        $tarefas = $this->tarefas->listar(
            isset($filtros['idProjeto']) ? (int) $filtros['idProjeto'] : null,
            isset($filtros['idResponsavel']) ? (int) $filtros['idResponsavel'] : null,
            isset($filtros['status']) ? TarefaStatus::from($filtros['status']) : null,
            $this->principal($request),
        );

        return response()->json(TarefaResource::collection($tarefas));
    }

    public function show(Request $request, int $id): JsonResponse
    {
        return response()->json(new TarefaResource($this->tarefas->detalhar($id, $this->principal($request))));
    }

    public function update(TarefaRequest $request, int $id): JsonResponse
    {
        $tarefa = $this->tarefas->atualizar($id, $request->validated(), $this->principal($request), $this->projetos);

        return response()->json(new TarefaResource($tarefa));
    }

    public function alterarStatus(TarefaStatusRequest $request, int $id): JsonResponse
    {
        $dados = $request->validated();

        $tarefa = $this->tarefas->alterarStatus(
            $id, TarefaStatus::from($dados['status']), $dados['dataConclusao'] ?? null, $this->principal($request),
        );

        return response()->json(new TarefaResource($tarefa));
    }

    public function concluir(Request $request, int $id): JsonResponse
    {
        $tarefa = $this->tarefas->concluirPropria($id, $this->principal($request));

        return response()->json(new TarefaResource($tarefa));
    }

    public function destroy(Request $request, int $id): JsonResponse
    {
        $this->tarefas->remover($id, $this->principal($request));

        return response()->json(null, 204);
    }
}
