<?php

namespace App\Modules\Vendas\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Vendas\Http\Requests\TarefaAtualizacaoRequest;
use App\Modules\Vendas\Http\Requests\TarefaRequest;
use App\Modules\Vendas\Http\Resources\TarefaResource;
use App\Modules\Vendas\Services\CrmService;
use App\Modules\Vendas\VendasPrincipal;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Tarefas de marketing — só HTTP. */
class TarefaMarketingController
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

    private function principal(Request $request): VendasPrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return VendasPrincipal::from($login);
    }
}
