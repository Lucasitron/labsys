<?php

namespace App\Http\Controllers\Vendas;

use App\Http\Controllers\Controller;
use App\Http\Requests\Vendas\InteracaoRequest;
use App\Http\Resources\Vendas\InteracaoResource;
use App\Services\Vendas\CrmService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Interações do CRM — só HTTP (escopo por cliente, sem list-all global). */
class InteracaoController extends Controller
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
}
