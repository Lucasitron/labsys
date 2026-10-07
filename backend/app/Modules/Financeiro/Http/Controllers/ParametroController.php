<?php

namespace App\Modules\Financeiro\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Financeiro\FinanceiroPrincipal;
use App\Modules\Financeiro\Http\Requests\OverheadRequest;
use App\Modules\Financeiro\Http\Requests\ValorHoraRequest;
use App\Modules\Financeiro\Http\Resources\OverheadResource;
use App\Modules\Financeiro\Http\Resources\ValorHoraResource;
use App\Modules\Financeiro\Services\ParametroService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Valores/hora por nível e taxa de overhead — só HTTP (append-only). */
class ParametroController
{
    public function __construct(private ParametroService $parametros) {}

    public function definirValorHora(ValorHoraRequest $request): JsonResponse
    {
        $valor = $this->parametros->definirValorHora($request->validated(), $this->principal($request));

        return response()->json((new ValorHoraResource($valor))->toArray($request), 201);
    }

    public function valoresVigentes(Request $request): JsonResponse
    {
        $valores = $this->parametros->valoresVigentes($this->principal($request));

        return response()->json(array_map(
            fn ($v) => (new ValorHoraResource($v))->toArray($request),
            $valores,
        ));
    }

    public function definirOverhead(OverheadRequest $request): JsonResponse
    {
        $overhead = $this->parametros->definirOverhead($request->validated(), $this->principal($request));

        return response()->json((new OverheadResource($overhead))->toArray($request), 201);
    }

    public function overheadVigente(Request $request): JsonResponse
    {
        $overhead = $this->parametros->overheadVigente($this->principal($request));

        return response()->json((new OverheadResource($overhead))->toArray($request));
    }

    private function principal(Request $request): FinanceiroPrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return FinanceiroPrincipal::from($login);
    }
}
