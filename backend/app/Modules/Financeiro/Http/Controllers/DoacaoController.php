<?php

namespace App\Modules\Financeiro\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Financeiro\Enums\TipoDoacao;
use App\Modules\Financeiro\FinanceiroPrincipal;
use App\Modules\Financeiro\Http\Requests\DoacaoRequest;
use App\Modules\Financeiro\Http\Resources\DoacaoResource;
use App\Modules\Financeiro\Services\DoacaoService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Doações e recursos de projetos — só HTTP. */
class DoacaoController
{
    public function __construct(private DoacaoService $doacoes) {}

    public function store(DoacaoRequest $request): JsonResponse
    {
        $doacao = $this->doacoes->registrar($request->validated(), $this->principal($request));

        return response()->json((new DoacaoResource($doacao))->toArray($request), 201);
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'tipo' => ['nullable', 'string', 'in:DOACAO,PROJETO'],
            'dataInicio' => ['nullable', 'date'],
            'dataFim' => ['nullable', 'date'],
        ]);

        $doacoes = $this->doacoes->listar(
            isset($filtros['tipo']) ? TipoDoacao::from($filtros['tipo']) : null,
            $filtros['dataInicio'] ?? null,
            $filtros['dataFim'] ?? null,
            $this->principal($request),
        );

        return response()->json(array_map(
            fn ($d) => (new DoacaoResource($d))->toArray($request),
            $doacoes,
        ));
    }

    private function principal(Request $request): FinanceiroPrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return FinanceiroPrincipal::from($login);
    }
}
