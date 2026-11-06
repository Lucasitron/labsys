<?php

namespace App\Http\Controllers\Financeiro;

use App\Http\Controllers\Controller;

use App\Models\Auth\Login;
use App\Shared\Auth\Caller;
use App\Http\Requests\Financeiro\OverheadRequest;
use App\Http\Requests\Financeiro\ValorHoraRequest;
use App\Http\Resources\Financeiro\OverheadResource;
use App\Http\Resources\Financeiro\ValorHoraResource;
use App\Services\Financeiro\ParametroService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Valores/hora por nível e taxa de overhead — só HTTP (append-only). */
class ParametroController extends Controller
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

}
