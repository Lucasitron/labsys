<?php

namespace App\Http\Controllers\Rh;

use App\Http\Controllers\Controller;
use App\Http\Controllers\Rh\ResolvesRhPrincipal;

use App\Enums\Rh\StatusApontamento;
use App\Http\Requests\Rh\RegistrarApontamentoRequest;
use App\Http\Requests\Rh\ValidarApontamentoRequest;
use App\Http\Resources\Rh\ApontamentoResource;
use App\Services\Rh\ApontamentoService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Apontamentos — só HTTP. Validar: Admin ou tutor (service). */
class ApontamentoHorasController extends Controller
{
    use ResolvesRhPrincipal;
    public function __construct(private ApontamentoService $apontamentos) {}

    public function store(RegistrarApontamentoRequest $request): JsonResponse
    {

        $apontamento = $this->apontamentos->registrar($request->only([
            'idFuncionario', 'tipo', 'idReferencia', 'data', 'horasTrabalhadas',
            'horaInicio', 'horaFim', 'descricaoAtividade',
        ]), $this->principal($request));

        return response()->json((new ApontamentoResource($apontamento))->toArray($request), 201);
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'status' => ['nullable', 'in:PENDENTE,VALIDADO,REJEITADO'],
            'periodo' => ['nullable', 'date_format:Y-m'],
        ]);


        $lista = $this->apontamentos->listar(
            isset($filtros['status']) ? StatusApontamento::from($filtros['status']) : null,
            $filtros['periodo'] ?? null,
            $this->principal($request),
        );

        return response()->json(collect($lista)->map(
            fn ($a) => (new ApontamentoResource($a))->toArray($request)
        )->all());
    }

    public function validar(ValidarApontamentoRequest $request, int $id): JsonResponse
    {

        $apontamento = $this->apontamentos->validar(
            $id,
            StatusApontamento::from($request->input('status')),
            $request->input('motivo'),
            $this->principal($request),
        );

        return response()->json((new ApontamentoResource($apontamento))->toArray($request));
    }
}
