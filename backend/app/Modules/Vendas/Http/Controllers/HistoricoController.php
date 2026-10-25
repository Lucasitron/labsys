<?php

namespace App\Modules\Vendas\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Vendas\Http\Resources\HistoricoResource;
use App\Modules\Vendas\Services\EncomendaService;
use App\Modules\Vendas\VendasPrincipal;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Histórico do Kanban — leitura (sem service dedicado). */
class HistoricoController
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

    private function principal(Request $request): VendasPrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return VendasPrincipal::from($login);
    }
}
