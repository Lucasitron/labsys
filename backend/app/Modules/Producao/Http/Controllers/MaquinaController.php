<?php

namespace App\Modules\Producao\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Producao\Enums\MaquinaStatus;
use App\Modules\Producao\Http\Requests\MaquinaRequest;
use App\Modules\Producao\Http\Requests\MaquinaStatusRequest;
use App\Modules\Producao\Http\Requests\UsoEncerrarRequest;
use App\Modules\Producao\Http\Requests\UsoIniciarRequest;
use App\Modules\Producao\Http\Resources\HistoricoUsoResource;
use App\Modules\Producao\Http\Resources\MaquinaResource;
use App\Modules\Producao\ProducaoPrincipal;
use App\Modules\Producao\Services\MaquinaService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Máquinas e usos — só HTTP (cadastro/status Admin; `horas_uso` no service). */
class MaquinaController
{
    public function __construct(private MaquinaService $maquinas) {}

    public function store(MaquinaRequest $request): JsonResponse
    {
        $maquina = $this->maquinas->criar($request->validated(), $this->principal($request));

        return response()->json(new MaquinaResource($maquina), 201);
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'status' => ['nullable', 'string', 'in:DISPONIVEL,EM_USO,MANUTENCAO'],
        ]);

        $maquinas = $this->maquinas->listar(
            isset($filtros['status']) ? MaquinaStatus::from($filtros['status']) : null,
            $this->principal($request),
        );

        return response()->json(MaquinaResource::collection($maquinas));
    }

    public function show(Request $request, int $id): JsonResponse
    {
        return response()->json(new MaquinaResource($this->maquinas->detalhar($id, $this->principal($request))));
    }

    public function update(MaquinaRequest $request, int $id): JsonResponse
    {
        $maquina = $this->maquinas->atualizar($id, $request->validated(), $this->principal($request));

        return response()->json(new MaquinaResource($maquina));
    }

    public function alterarStatus(MaquinaStatusRequest $request, int $id): JsonResponse
    {
        $maquina = $this->maquinas->alterarStatus(
            $id, MaquinaStatus::from($request->validated()['status']), $this->principal($request),
        );

        return response()->json(new MaquinaResource($maquina));
    }

    public function iniciarUso(UsoIniciarRequest $request, int $id): JsonResponse
    {
        $uso = $this->maquinas->iniciarUso($id, $request->validated(), $this->principal($request));

        return response()->json(new HistoricoUsoResource($uso), 201);
    }

    public function encerrarUso(UsoEncerrarRequest $request, int $id, int $idUso): JsonResponse
    {
        $uso = $this->maquinas->encerrarUso($id, $idUso, $request->validated(), $this->principal($request));

        return response()->json(new HistoricoUsoResource($uso));
    }

    public function historico(Request $request, int $id): JsonResponse
    {
        $usos = $this->maquinas->historico($id, $this->principal($request));

        return response()->json(HistoricoUsoResource::collection($usos));
    }

    public function destroy(Request $request, int $id): JsonResponse
    {
        $this->maquinas->remover($id, $this->principal($request));

        return response()->json(null, 204);
    }

    private function principal(Request $request): ProducaoPrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return ProducaoPrincipal::from($login);
    }
}
