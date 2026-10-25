<?php

namespace App\Modules\Vendas\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Vendas\Enums\StatusKanban;
use App\Modules\Vendas\Http\Requests\EncomendaRequest;
use App\Modules\Vendas\Http\Requests\KanbanRequest;
use App\Modules\Vendas\Http\Requests\NovaOrdemRequest;
use App\Modules\Vendas\Http\Resources\EncomendaResource;
use App\Modules\Vendas\Services\EncomendaService;
use App\Modules\Vendas\VendasPrincipal;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Encomendas e Kanban — só HTTP (criador+Admin e `versao` no service). */
class EncomendaController
{
    public function __construct(private EncomendaService $encomendas) {}

    public function store(EncomendaRequest $request): JsonResponse
    {
        $encomenda = $this->encomendas->criar($request->validated(), $this->principal($request));

        return response()->json($this->detalhe($request, $encomenda->getKey()), 201);
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'status' => ['nullable', 'string', 'in:Fila,Produção,Acabamento,Pronto,Entregue'],
            'clienteId' => ['nullable', 'integer'],
        ]);

        $dados = $this->encomendas->listar(
            isset($filtros['status']) ? StatusKanban::from($filtros['status']) : null,
            isset($filtros['clienteId']) ? (int) $filtros['clienteId'] : null,
            $this->principal($request),
        );

        return response()->json([
            'encomendas' => array_map(
                fn ($e) => (new EncomendaResource($e))->toArray($request),
                $dados['encomendas'],
            ),
            'counts' => $dados['counts'],
        ]);
    }

    public function show(Request $request, int $id): JsonResponse
    {
        return response()->json($this->detalhe($request, $id));
    }

    public function status(Request $request, int $id): JsonResponse
    {
        $encomenda = $this->encomendas->detalhar($id, $this->principal($request));

        return response()->json([
            'id' => (int) $encomenda->getKey(),
            'statusKanban' => $encomenda->status_kanban->value,
            'dataPrevisaoEntrega' => $encomenda->data_previsao_entrega === null
                ? null : substr((string) $encomenda->data_previsao_entrega, 0, 10),
            'valorFinal' => (string) $encomenda->valor_final,
        ]);
    }

    public function moverKanban(KanbanRequest $request, int $id): JsonResponse
    {
        $dados = $request->validated();

        $encomenda = $this->encomendas->moverKanban(
            $id,
            StatusKanban::from($dados['statusKanban']),
            (int) $dados['versao'],
            $dados['observacao'] ?? null,
            $this->principal($request),
        );

        return response()->json($this->detalhe($request, $encomenda->getKey()));
    }

    public function novaOrdem(NovaOrdemRequest $request, int $id): JsonResponse
    {
        $nova = $this->encomendas->novaOrdem($id, $request->validated(), $this->principal($request));

        return response()->json($this->detalhe($request, $nova->getKey()), 201);
    }

    /** @return array<string, mixed> */
    private function detalhe(Request $request, int $id): array
    {
        $principal = $this->principal($request);
        $encomenda = $this->encomendas->detalhar($id, $principal);

        $resource = new EncomendaResource($encomenda);
        $resource->itensOrcamento = $this->encomendas->itensDe($id, $principal);

        return $resource->toArray($request);
    }

    private function principal(Request $request): VendasPrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return VendasPrincipal::from($login);
    }
}
