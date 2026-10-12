<?php

namespace App\Modules\Financeiro\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Financeiro\FinanceiroPrincipal;
use App\Modules\Financeiro\Http\Requests\LancamentoRequest;
use App\Modules\Financeiro\Http\Requests\PagamentoRequest;
use App\Modules\Financeiro\Http\Resources\LancamentoResource;
use App\Modules\Financeiro\Services\LancamentoService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Lançamentos (contas a pagar/receber) — só HTTP. Sem endpoint bulk. */
class LancamentoController
{
    public function __construct(private LancamentoService $lancamentos) {}

    public function store(LancamentoRequest $request): JsonResponse
    {
        $lancamento = $this->lancamentos->criar($request->validated(), $this->principal($request));

        return response()->json((new LancamentoResource($lancamento))->toArray($request), 201);
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'status' => ['nullable', 'string', 'in:PENDENTE,PAGO,ATRASADO,CANCELADO'],
            'tipo' => ['nullable', 'string', 'in:ENTRADA,SAIDA'],
            'idCategoria' => ['nullable', 'integer'],
            'dataInicio' => ['nullable', 'date'],
            'dataFim' => ['nullable', 'date'],
            'origem' => ['nullable', 'string', 'max:100'],
            'vencimentoDe' => ['nullable', 'date'],
            'vencimentoAte' => ['nullable', 'date'],
        ]);

        $dados = $this->lancamentos->listar($filtros, $this->principal($request));

        return response()->json([
            'lancamentos' => array_map(
                fn ($l) => (new LancamentoResource($l))->toArray($request),
                $dados['lancamentos'],
            ),
            'counts' => $dados['counts'],
            'resumo' => $dados['resumo'],
        ]);
    }

    public function pagar(PagamentoRequest $request, int $id): JsonResponse
    {
        $lancamento = $this->lancamentos->registrarPagamento(
            $id, $request->validated(), $this->principal($request),
        );

        return response()->json((new LancamentoResource($lancamento))->toArray($request));
    }

    private function principal(Request $request): FinanceiroPrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return FinanceiroPrincipal::from($login);
    }
}
