<?php

namespace App\Http\Controllers\Rh;

use App\Http\Controllers\Controller;
use App\Http\Controllers\Rh\Concerns\ResolvesRhPrincipal;

use App\Enums\Rh\StatusApontamento;
use App\Http\Requests\Rh\RegistrarApontamentoRequest;
use App\Http\Requests\Rh\RejeitarApontamentoRequest;
use App\Http\Resources\Rh\ApontamentoResource;
use App\Services\Rh\ApontamentoService;
use App\Services\Rh\CertificadoService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/**
 * Fachada de horas (tela de registro): delega ao ApontamentoService e ao
 * CertificadoService — sem classe fachada extra.
 */
class HorasController extends Controller
{
    use ResolvesRhPrincipal;
    public function __construct(
        private ApontamentoService $apontamentos,
        private CertificadoService $certificados,
    ) {}

    public function disponiveis(Request $request): JsonResponse
    {

        return response()->json($this->certificados->horasDisponiveis($this->principal($request)));
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

    public function store(RegistrarApontamentoRequest $request): JsonResponse
    {

        $apontamento = $this->apontamentos->registrar($request->only([
            'idFuncionario', 'tipo', 'idReferencia', 'data', 'horasTrabalhadas',
            'horaInicio', 'horaFim', 'descricaoAtividade',
        ]), $this->principal($request));

        return response()->json((new ApontamentoResource($apontamento))->toArray($request), 201);
    }

    public function validar(Request $request, int $id): JsonResponse
    {

        $apontamento = $this->apontamentos->validar(
            $id, StatusApontamento::VALIDADO, null, $this->principal($request),
        );

        return response()->json((new ApontamentoResource($apontamento))->toArray($request));
    }

    public function rejeitar(RejeitarApontamentoRequest $request, int $id): JsonResponse
    {

        $apontamento = $this->apontamentos->validar(
            $id, StatusApontamento::REJEITADO, $request->input('motivo'), $this->principal($request),
        );

        return response()->json((new ApontamentoResource($apontamento))->toArray($request));
    }
}
