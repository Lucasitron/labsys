<?php

namespace App\Http\Controllers\Rh;

use App\Enums\Rh\NivelAcesso;
use App\Http\Controllers\Controller;
use App\Http\Requests\Rh\AlterarNivelRequest;
use App\Http\Requests\Rh\ConvidarRequest;
use App\Http\Resources\Rh\FuncionarioResource;
use App\Services\Rh\NivelService;
use Illuminate\Http\JsonResponse;

/** Níveis (Admin): matriz, alteração de membro e convites. Só HTTP. */
class NivelController extends Controller
{
    use ResolvesRhPrincipal;

    public function __construct(private NivelService $niveis) {}

    public function matriz(): JsonResponse
    {
        return response()->json($this->niveis->matriz());
    }

    public function alterarMembro(AlterarNivelRequest $request, int $id): JsonResponse
    {

        return response()->json($this->niveis->alterarMembro(
            $id,
            NivelAcesso::from((int) $request->input('nivel')),
            $this->principal($request),
        ));
    }

    public function convidar(ConvidarRequest $request): JsonResponse
    {
        $funcionario = $this->niveis->convidar([
            'nomeCompleto' => $request->input('nomeCompleto'),
            'matricula' => $request->input('matricula'),
            'contato' => $request->input('contato'),
            'departamento' => $request->input('departamento'),
            'nivelAcesso' => $request->input('nivelAcesso') !== null
                ? NivelAcesso::from((int) $request->input('nivelAcesso'))
                : null,
        ], $this->principal($request));

        return response()->json(
            (new FuncionarioResource($funcionario->load('pessoa')))->toArray($request),
            201
        );
    }
}
