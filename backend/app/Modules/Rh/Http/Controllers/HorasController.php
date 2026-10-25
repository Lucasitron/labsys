<?php

namespace App\Modules\Rh\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Rh\Enums\StatusApontamento;
use App\Modules\Rh\Http\Requests\RegistrarApontamentoRequest;
use App\Modules\Rh\Http\Requests\RejeitarApontamentoRequest;
use App\Modules\Rh\Http\Resources\ApontamentoResource;
use App\Modules\Rh\RhPrincipal;
use App\Modules\Rh\Services\ApontamentoService;
use App\Modules\Rh\Services\CertificadoService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/**
 * Fachada de horas (tela de registro): delega ao ApontamentoService e ao
 * CertificadoService — sem classe fachada extra.
 */
class HorasController
{
    public function __construct(
        private ApontamentoService $apontamentos,
        private CertificadoService $certificados,
    ) {}

    public function disponiveis(Request $request): JsonResponse
    {
        /** @var Login $login */
        $login = $request->user();

        return response()->json($this->certificados->horasDisponiveis(RhPrincipal::from($login)));
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'status' => ['nullable', 'in:PENDENTE,VALIDADO,REJEITADO'],
            'periodo' => ['nullable', 'regex:/^\d{4}-\d{2}$/'],
        ]);

        /** @var Login $login */
        $login = $request->user();

        $lista = $this->apontamentos->listar(
            isset($filtros['status']) ? StatusApontamento::from($filtros['status']) : null,
            $filtros['periodo'] ?? null,
            RhPrincipal::from($login),
        );

        return response()->json(collect($lista)->map(
            fn ($a) => (new ApontamentoResource($a))->toArray($request)
        )->all());
    }

    public function store(RegistrarApontamentoRequest $request): JsonResponse
    {
        /** @var Login $login */
        $login = $request->user();

        $apontamento = $this->apontamentos->registrar($request->only([
            'idFuncionario', 'tipo', 'idReferencia', 'data', 'horasTrabalhadas',
            'horaInicio', 'horaFim', 'descricaoAtividade',
        ]), RhPrincipal::from($login));

        return response()->json((new ApontamentoResource($apontamento))->toArray($request), 201);
    }

    public function validar(Request $request, int $id): JsonResponse
    {
        /** @var Login $login */
        $login = $request->user();

        $apontamento = $this->apontamentos->validar(
            $id, StatusApontamento::VALIDADO, null, RhPrincipal::from($login),
        );

        return response()->json((new ApontamentoResource($apontamento))->toArray($request));
    }

    public function rejeitar(RejeitarApontamentoRequest $request, int $id): JsonResponse
    {
        /** @var Login $login */
        $login = $request->user();

        $apontamento = $this->apontamentos->validar(
            $id, StatusApontamento::REJEITADO, $request->input('motivo'), RhPrincipal::from($login),
        );

        return response()->json((new ApontamentoResource($apontamento))->toArray($request));
    }
}
