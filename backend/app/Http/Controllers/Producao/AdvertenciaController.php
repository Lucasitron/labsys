<?php

namespace App\Http\Controllers\Producao;

use App\Http\Controllers\Controller;
use App\Http\Requests\Producao\AdvertenciaRequest;
use App\Http\Resources\Producao\AdvertenciaResource;
use App\Services\Producao\AdvertenciaService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Advertências — só HTTP (registro Admin; leitura própria p/ não-Admin). */
class AdvertenciaController extends Controller
{
    public function __construct(private AdvertenciaService $advertencias) {}

    public function store(AdvertenciaRequest $request): JsonResponse
    {
        $advertencia = $this->advertencias->registrar($request->validated(), $this->principal($request));

        return response()->json(new AdvertenciaResource($advertencia), 201);
    }

    public function index(Request $request): JsonResponse
    {
        $lista = $this->advertencias->listar(null, $this->principal($request));

        return response()->json(AdvertenciaResource::collection($lista));
    }

    public function porFuncionario(Request $request, int $idFuncionario): JsonResponse
    {
        $lista = $this->advertencias->listar($idFuncionario, $this->principal($request));

        return response()->json(AdvertenciaResource::collection($lista));
    }
}
