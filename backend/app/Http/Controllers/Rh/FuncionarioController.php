<?php

namespace App\Http\Controllers\Rh;

use App\Http\Controllers\Controller;
use App\Http\Controllers\Rh\ResolvesRhPrincipal;

use App\Enums\Rh\NivelAcesso;
use App\Http\Requests\Rh\AlterarNivelRequest;
use App\Http\Requests\Rh\VincularFuncionarioRequest;
use App\Http\Resources\Rh\FuncionarioResource;
use App\Services\Rh\FuncionarioService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Funcionários — só HTTP. Nível e vínculo são Admin (rota `can:admin`). */
class FuncionarioController extends Controller
{
    use ResolvesRhPrincipal;
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

        return response()->json($this->funcionarios->alterarNivel(
            $id,
            NivelAcesso::from((int) $request->input('nivel')),
            $this->principal($request),
        ));
    }

    public function horas(Request $request, int $id): JsonResponse
    {

        return response()->json($this->funcionarios->totalHoras($id, $this->principal($request)));
    }
}
