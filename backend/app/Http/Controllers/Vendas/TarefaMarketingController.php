<?php

namespace App\Http\Controllers\Vendas;

use App\Http\Controllers\Controller;
use App\Http\Requests\Vendas\TarefaAtualizacaoRequest;
use App\Http\Requests\Vendas\TarefaRequest;
use App\Http\Resources\Vendas\TarefaResource;
use App\Services\Vendas\CrmService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Tarefas de marketing — só HTTP. */
class TarefaMarketingController extends Controller
{
    public function __construct(private CrmService $crm) {}

    public function store(TarefaRequest $request): JsonResponse
    {
        $tarefa = $this->crm->criarTarefa($request->validated(), $this->principal($request));

        return response()->json((new TarefaResource($tarefa))->toArray($request), 201);
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'responsavelId' => ['nullable', 'integer'],
            'status' => ['nullable', 'string', 'in:Pendente,Em Andamento,Concluída'],
        ]);

        $dados = $this->crm->listarTarefas(
            isset($filtros['responsavelId']) ? (int) $filtros['responsavelId'] : null,
            $filtros['status'] ?? null,
            $this->principal($request),
        );

        return response()->json([
            'tarefas' => array_map(
                fn ($t) => (new TarefaResource($t))->toArray($request),
                $dados['tarefas'],
            ),
            'counts' => $dados['counts'],
        ]);
    }

    public function update(TarefaAtualizacaoRequest $request, int $id): JsonResponse
    {
        $tarefa = $this->crm->atualizarTarefa($id, $request->validated(), $this->principal($request));

        return response()->json((new TarefaResource($tarefa))->toArray($request));
    }
}
