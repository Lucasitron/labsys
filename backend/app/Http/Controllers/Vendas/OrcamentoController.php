<?php

namespace App\Http\Controllers\Vendas;

use App\Enums\Vendas\StatusOrcamento;
use App\Http\Controllers\Controller;
use App\Http\Requests\Vendas\OrcamentoAtualizacaoRequest;
use App\Http\Requests\Vendas\OrcamentoRequest;
use App\Http\Resources\Vendas\OrcamentoResource;
use App\Services\Vendas\OrcamentoService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Orçamentos — só HTTP. Conversão em encomenda é o POST /encomendas. */
class OrcamentoController extends Controller
{
    public function __construct(private OrcamentoService $orcamentos) {}

    public function store(OrcamentoRequest $request): JsonResponse
    {
        $orcamento = $this->orcamentos->criar($request->validated(), $this->principal($request));

        return response()->json((new OrcamentoResource($orcamento))->toArray($request), 201);
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'status' => ['nullable', 'string', 'in:Pendente,Aprovado,Recusado,Ajuste'],
            'clienteId' => ['nullable', 'integer'],
        ]);

        $dados = $this->orcamentos->listar(
            isset($filtros['status']) ? StatusOrcamento::from($filtros['status']) : null,
            isset($filtros['clienteId']) ? (int) $filtros['clienteId'] : null,
            $this->principal($request),
        );

        return response()->json([
            'orcamentos' => array_map(
                fn ($o) => (new OrcamentoResource($o))->toArray($request),
                $dados['orcamentos'],
            ),
            'counts' => $dados['counts'],
        ]);
    }

    public function show(Request $request, int $id): OrcamentoResource
    {
        return new OrcamentoResource($this->orcamentos->detalhar($id, $this->principal($request)));
    }

    public function update(OrcamentoAtualizacaoRequest $request, int $id): JsonResponse
    {
        $orcamento = $this->orcamentos->atualizar($id, $request->validated(), $this->principal($request));

        return response()->json((new OrcamentoResource($orcamento))->toArray($request));
    }

    public function duplicar(Request $request, int $id): JsonResponse
    {
        $copia = $this->orcamentos->duplicar($id, $this->principal($request));

        return response()->json((new OrcamentoResource($copia))->toArray($request), 201);
    }
}
