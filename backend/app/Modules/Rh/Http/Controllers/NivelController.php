<?php

namespace App\Modules\Rh\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Rh\Enums\NivelAcesso;
use App\Modules\Rh\Http\Requests\AlterarNivelRequest;
use App\Modules\Rh\Http\Requests\ConvidarRequest;
use App\Modules\Rh\Http\Resources\FuncionarioResource;
use App\Modules\Rh\RhPrincipal;
use App\Modules\Rh\Services\NivelService;
use Illuminate\Http\JsonResponse;

/** Níveis (Admin): matriz, alteração de membro e convites. Só HTTP. */
class NivelController
{
    public function __construct(private NivelService $niveis) {}

    public function matriz(): JsonResponse
    {
        return response()->json($this->niveis->matriz());
    }

    public function alterarMembro(AlterarNivelRequest $request, int $id): JsonResponse
    {
        /** @var Login $login */
        $login = $request->user();

        return response()->json($this->niveis->alterarMembro(
            $id,
            NivelAcesso::from((int) $request->input('nivel')),
            RhPrincipal::from($login),
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
        ]);

        return response()->json(
            (new FuncionarioResource($funcionario->load('pessoa')))->toArray($request),
            201
        );
    }
}
