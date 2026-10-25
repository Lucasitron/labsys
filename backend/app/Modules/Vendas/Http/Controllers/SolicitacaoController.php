<?php

namespace App\Modules\Vendas\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Vendas\Enums\StatusSolicitacao;
use App\Modules\Vendas\Http\Requests\DecisaoRequest;
use App\Modules\Vendas\Http\Requests\SolicitacaoRequest;
use App\Modules\Vendas\Http\Resources\SolicitacaoResource;
use App\Modules\Vendas\Services\SolicitacaoService;
use App\Modules\Vendas\VendasPrincipal;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Solicitações de edição — só HTTP (pedido aberto; decisão Admin, sem auto-apply). */
class SolicitacaoController
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

    private function principal(Request $request): VendasPrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return VendasPrincipal::from($login);
    }
}
