<?php

namespace App\Http\Controllers\Vendas;

use App\Http\Controllers\Controller;

use App\Models\Auth\Login;
use App\Http\Requests\Vendas\MarketplaceRequest;
use App\Http\Resources\Vendas\MarketplaceResource;
use App\Services\Vendas\MarketplaceService;
use App\Shared\Auth\Caller;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Marketplace manual — só HTTP (só `?encomendaId=&plataforma=` no listar). */
class MarketplaceController extends Controller
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

}
