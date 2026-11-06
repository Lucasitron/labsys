<?php

namespace App\Http\Controllers\Vendas;

use App\Http\Controllers\Controller;

use App\Models\Auth\Login;
use App\Enums\Vendas\StatusSolicitacao;
use App\Http\Requests\Vendas\DecisaoRequest;
use App\Http\Requests\Vendas\SolicitacaoRequest;
use App\Http\Resources\Vendas\SolicitacaoResource;
use App\Services\Vendas\SolicitacaoService;
use App\Shared\Auth\Caller;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Solicitações de edição — só HTTP (pedido aberto; decisão Admin, sem auto-apply). */
class SolicitacaoController extends Controller
{
    public function __construct(private SolicitacaoService $solicitacoes) {}

    public function store(SolicitacaoRequest $request): JsonResponse
    {
        $solicitacao = $this->solicitacoes->solicitar($request->validated(), $this->principal($request));

        return response()->json((new SolicitacaoResource($solicitacao))->toArray($request), 201);
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'status' => ['nullable', 'string', 'in:Pendente,Aprovada,Rejeitada'],
        ]);

        $dados = $this->solicitacoes->listar(
            isset($filtros['status']) ? StatusSolicitacao::from($filtros['status']) : null,
            $this->principal($request),
        );

        return response()->json([
            'solicitacoes' => array_map(
                fn ($s) => (new SolicitacaoResource($s))->toArray($request),
                $dados['solicitacoes'],
            ),
            'counts' => $dados['counts'],
        ]);
    }

    public function decidir(DecisaoRequest $request, int $id): JsonResponse
    {
        $dados = $request->validated();

        $solicitacao = $this->solicitacoes->decidir(
            $id,
            (string) $dados['decisao'],
            $dados['motivo'] ?? null,
            $this->principal($request),
        );

        return response()->json((new SolicitacaoResource($solicitacao))->toArray($request));
    }

}
