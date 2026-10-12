<?php

namespace App\Modules\Rh\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Rh\Http\Requests\AvaliarTreinamentoRequest;
use App\Modules\Rh\Http\Requests\CriarTreinamentoRequest;
use App\Modules\Rh\Http\Resources\AvaliacaoResource;
use App\Modules\Rh\Http\Resources\TreinamentoResource;
use App\Modules\Rh\RhPrincipal;
use App\Modules\Rh\Services\TreinamentoService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Treinamentos LMS — só HTTP. */
class TreinamentoController
{
    public function __construct(private TreinamentoService $treinamentos) {}

    public function store(CriarTreinamentoRequest $request): JsonResponse
    {
        /** @var Login $login */
        $login = $request->user();

        $treinamento = $this->treinamentos->criar($request->only([
            'titulo', 'descricao', 'urlConteudo', 'idTutor',
        ]), RhPrincipal::from($login));

        return response()->json((new TreinamentoResource($treinamento))->toArray($request), 201);
    }

    public function avaliar(AvaliarTreinamentoRequest $request, int $id): JsonResponse
    {
        /** @var Login $login */
        $login = $request->user();

        $avaliacao = $this->treinamentos->avaliar($id, $request->only([
            'idFuncionario', 'nota', 'feedback',
        ]), RhPrincipal::from($login));

        return response()->json((new AvaliacaoResource($avaliacao))->toArray($request), 201);
    }

    public function avaliacoes(Request $request, int $id): JsonResponse
    {
        return response()->json(collect($this->treinamentos->listarAvaliacoes($id))->map(
            fn ($a) => (new AvaliacaoResource($a))->toArray($request)
        )->all());
    }
}
