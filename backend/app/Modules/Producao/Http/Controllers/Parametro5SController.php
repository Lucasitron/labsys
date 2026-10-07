<?php

namespace App\Modules\Producao\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Producao\Http\Requests\ParametroRequest;
use App\Modules\Producao\Http\Resources\ParametroResource;
use App\Modules\Producao\Models\Parametro5S;
use App\Modules\Producao\Policies\ProducaoPolicy;
use App\Modules\Producao\ProducaoPrincipal;
use App\Shared\Exceptions\ResourceNotFoundException;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Parâmetros 5S — só HTTP (regra trivial no Model, sem service; `PUT` é Admin). */
class Parametro5SController
{
    public function index(Request $request): JsonResponse
    {
        ProducaoPolicy::exigeLeitura($this->principal($request));

        return response()->json(ParametroResource::collection(Parametro5S::orderBy('id_parametro')->get()));
    }

    public function show(Request $request, int $id): JsonResponse
    {
        ProducaoPolicy::exigeLeitura($this->principal($request));

        return response()->json(new ParametroResource($this->obter($id)));
    }

    public function update(ParametroRequest $request, int $id): JsonResponse
    {
        $principal = $this->principal($request);
        ProducaoPolicy::exigeAdmin($principal);

        $parametro = $this->obter($id);
        $parametro->update($request->validated());

        return response()->json(new ParametroResource($parametro->refresh()));
    }

    private function obter(int $id): Parametro5S
    {
        return Parametro5S::find($id)
            ?? throw new ResourceNotFoundException("Parâmetro 5S não encontrado: {$id}");
    }

    private function principal(Request $request): ProducaoPrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return ProducaoPrincipal::from($login);
    }
}
