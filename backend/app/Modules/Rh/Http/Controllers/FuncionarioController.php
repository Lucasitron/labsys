<?php

namespace App\Modules\Rh\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Rh\Enums\NivelAcesso;
use App\Modules\Rh\Http\Requests\AlterarNivelRequest;
use App\Modules\Rh\Http\Requests\VincularFuncionarioRequest;
use App\Modules\Rh\Http\Resources\FuncionarioResource;
use App\Modules\Rh\RhPrincipal;
use App\Modules\Rh\Services\FuncionarioService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Funcionários — só HTTP. Nível e vínculo são Admin (rota `can:admin`). */
class FuncionarioController
{
    public function __construct(private FuncionarioService $funcionarios) {}

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'nivel' => ['nullable'],
            'departamento' => ['nullable', 'string', 'max:255'],
        ]);

        $lista = $this->funcionarios->listar(
            isset($filtros['nivel']) ? NivelAcesso::parse($filtros['nivel']) : null,
            $filtros['departamento'] ?? null,
        );

        return response()->json(collect($lista)->map(
            fn ($f) => (new FuncionarioResource($f))->toArray($request)
        )->all());
    }

    public function store(VincularFuncionarioRequest $request): JsonResponse
    {
        $funcionario = $this->funcionarios->vincular([
            'id_pessoa' => (int) $request->input('idPessoa'),
            'nivel_acesso' => $request->input('nivelAcesso') !== null
                ? NivelAcesso::from((int) $request->input('nivelAcesso'))
                : null,
            'departamento' => $request->input('departamento'),
        ]);

        return response()->json(
            (new FuncionarioResource($funcionario->load('pessoa')))->toArray($request),
            201
        );
    }

    public function alterarNivel(AlterarNivelRequest $request, int $id): JsonResponse
    {
        /** @var Login $login */
        $login = $request->user();

        return response()->json($this->funcionarios->alterarNivel(
            $id,
            NivelAcesso::from((int) $request->input('nivel')),
            RhPrincipal::from($login),
        ));
    }

    public function horas(Request $request, int $id): JsonResponse
    {
        /** @var Login $login */
        $login = $request->user();

        return response()->json($this->funcionarios->totalHoras($id, RhPrincipal::from($login)));
    }
}
