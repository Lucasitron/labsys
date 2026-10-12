<?php

namespace App\Modules\Vendas\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Vendas\Http\Requests\MarketplaceRequest;
use App\Modules\Vendas\Http\Resources\MarketplaceResource;
use App\Modules\Vendas\Services\MarketplaceService;
use App\Modules\Vendas\VendasPrincipal;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Marketplace manual — só HTTP (só `?encomendaId=&plataforma=` no listar). */
class MarketplaceController
{
    public function __construct(private MarketplaceService $marketplace) {}

    public function store(MarketplaceRequest $request): JsonResponse
    {
        $registro = $this->marketplace->registrar(
            $request->validated(),
            $this->principal($request),
        )->load(['encomenda.cliente']);

        return response()->json((new MarketplaceResource($registro))->toArray($request), 201);
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'encomendaId' => ['nullable', 'integer'],
            'plataforma' => ['nullable', 'string', 'max:64'],
        ]);

        $dados = $this->marketplace->listar(
            isset($filtros['encomendaId']) ? (int) $filtros['encomendaId'] : null,
            $filtros['plataforma'] ?? null,
            $this->principal($request),
        );

        foreach ($dados['registros'] as $registro) {
            $registro->loadMissing(['encomenda.cliente']);
        }

        return response()->json([
            'registros' => array_map(
                fn ($r) => (new MarketplaceResource($r))->toArray($request),
                $dados['registros'],
            ),
            'totais' => $dados['totais'],
        ]);
    }

    private function principal(Request $request): VendasPrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return VendasPrincipal::from($login);
    }
}
