<?php

namespace App\Modules\Rh\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Rh\Enums\StatusApontamento;
use App\Modules\Rh\Http\Requests\RegistrarApontamentoRequest;
use App\Modules\Rh\Http\Requests\ValidarApontamentoRequest;
use App\Modules\Rh\Http\Resources\ApontamentoResource;
use App\Modules\Rh\RhPrincipal;
use App\Modules\Rh\Services\ApontamentoService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Apontamentos — só HTTP. Validar: Admin ou tutor (service). */
class ApontamentoHorasController
{
    public function __construct(private ApontamentoService $apontamentos) {}

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

    public function validar(ValidarApontamentoRequest $request, int $id): JsonResponse
    {
        /** @var Login $login */
        $login = $request->user();

        $apontamento = $this->apontamentos->validar(
            $id,
            StatusApontamento::from($request->input('status')),
            $request->input('motivo'),
            RhPrincipal::from($login),
        );

        return response()->json((new ApontamentoResource($apontamento))->toArray($request));
    }
}
