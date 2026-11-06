<?php

namespace App\Http\Controllers\Vendas;

use App\Http\Controllers\Controller;

use App\Models\Auth\Login;
use App\Http\Resources\Vendas\HistoricoResource;
use App\Services\Vendas\EncomendaService;
use App\Shared\Auth\Caller;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Histórico do Kanban — leitura (sem service dedicado). */
class HistoricoController extends Controller
{
    public function __construct(private EncomendaService $encomendas) {}

    public function listar(Request $request, int $idEncomenda): JsonResponse
    {
        $historico = $this->encomendas->historico($idEncomenda, $this->principal($request));

        return response()->json(array_map(
            fn ($h) => (new HistoricoResource($h))->toArray($request),
            $historico,
        ));
    }

}
