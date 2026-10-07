<?php

namespace App\Modules\Producao\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Producao\Http\Requests\AdvertenciaRequest;
use App\Modules\Producao\Http\Resources\AdvertenciaResource;
use App\Modules\Producao\ProducaoPrincipal;
use App\Modules\Producao\Services\AdvertenciaService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Advertências — só HTTP (registro Admin; leitura própria p/ não-Admin). */
class AdvertenciaController
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

    private function principal(Request $request): ProducaoPrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return ProducaoPrincipal::from($login);
    }
}
