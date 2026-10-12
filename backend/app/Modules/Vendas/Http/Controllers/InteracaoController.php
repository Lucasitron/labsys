<?php

namespace App\Modules\Vendas\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Vendas\Http\Requests\InteracaoRequest;
use App\Modules\Vendas\Http\Resources\InteracaoResource;
use App\Modules\Vendas\Services\CrmService;
use App\Modules\Vendas\VendasPrincipal;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Interações do CRM — só HTTP (escopo por cliente, sem list-all global). */
class InteracaoController
{
    public function __construct(private CrmService $crm) {}

    public function store(InteracaoRequest $request): JsonResponse
    {
        $interacao = $this->crm->registrarInteracao($request->validated(), $this->principal($request));

        return response()->json((new InteracaoResource($interacao))->toArray($request), 201);
    }

    public function porCliente(Request $request, int $idCliente): JsonResponse
    {
        $interacoes = $this->crm->listarInteracoes($idCliente, $this->principal($request));

        return response()->json(array_map(
            fn ($i) => (new InteracaoResource($i))->toArray($request),
            $interacoes,
        ));
    }

    private function principal(Request $request): VendasPrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return VendasPrincipal::from($login);
    }
}
