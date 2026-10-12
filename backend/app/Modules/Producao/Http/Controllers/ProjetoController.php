<?php

namespace App\Modules\Producao\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Producao\Enums\ProjetoStatus;
use App\Modules\Producao\Http\Requests\ProjetoRequest;
use App\Modules\Producao\Http\Requests\ProjetoStatusRequest;
use App\Modules\Producao\Http\Resources\ProjetoResource;
use App\Modules\Producao\ProducaoPrincipal;
use App\Modules\Producao\Services\ProjetoService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Projetos — só HTTP (vínculos e carimbos no service). */
class ProjetoController
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

    private function principal(Request $request): ProducaoPrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return ProducaoPrincipal::from($login);
    }
}
