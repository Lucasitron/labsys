<?php

namespace App\Modules\Financeiro\Http\Controllers;

use App\Http\Controllers\Controller;

use App\Modules\Auth\Models\Login;
use App\Shared\Auth\Caller;
use App\Modules\Financeiro\Http\Requests\FechamentoRequest;
use App\Modules\Financeiro\Http\Resources\CustoResource;
use App\Modules\Financeiro\Http\Resources\FechamentoResource;
use App\Modules\Financeiro\Services\CusteioService;
use App\Modules\Financeiro\Services\FechamentoService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Fechamento de encomenda e consulta de custo — só HTTP. Sem PUT/cancel. */
class CusteioController extends Controller
{
    public function __construct(
        private FechamentoService $fechamentos,
        private CusteioService $custeio,
    ) {}

    public function fechar(FechamentoRequest $request): JsonResponse
    {
        $fechamento = $this->fechamentos->criar($request->validated(), $this->principal($request));

        return response()->json((new FechamentoResource($fechamento))->toArray($request), 201);
    }

    public function listar(Request $request): JsonResponse
    {
        $fechamentos = $this->fechamentos->listar($this->principal($request));

        return response()->json(array_map(
            fn ($f) => (new FechamentoResource($f))->toArray($request),
            $fechamentos,
        ));
    }

    public function consultar(Request $request, int $idEncomenda): JsonResponse
    {
        $fechamento = $this->fechamentos->consultar($idEncomenda, $this->principal($request));

        return response()->json((new FechamentoResource($fechamento))->toArray($request));
    }

    public function novaOrdem(FechamentoRequest $request, int $idEncomenda): JsonResponse
    {
        $nova = $this->fechamentos->novaOrdem($idEncomenda, $request->validated(), $this->principal($request));

        return response()->json((new FechamentoResource($nova))->toArray($request), 201);
    }

    public function custo(Request $request, int $idEncomenda): JsonResponse
    {
        // Último cálculo congelado (o cálculo é disparado por producao.concluida.event).
        $custo = $this->custeio->consultar($idEncomenda, $this->principal($request));

        return response()->json((new CustoResource($custo))->toArray($request));
    }

}
