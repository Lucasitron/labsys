<?php

namespace App\Http\Controllers\Rh;

use App\Http\Controllers\Controller;
use App\Http\Controllers\Rh\Concerns\ResolvesRhPrincipal;

use App\Http\Requests\Rh\AvaliarTreinamentoRequest;
use App\Http\Requests\Rh\CriarTreinamentoRequest;
use App\Http\Resources\Rh\AvaliacaoResource;
use App\Http\Resources\Rh\TreinamentoResource;
use App\Services\Rh\TreinamentoService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Treinamentos LMS — só HTTP. */
class TreinamentoController extends Controller
{
    use ResolvesRhPrincipal;
    public function __construct(private TreinamentoService $treinamentos) {}

    public function store(CriarTreinamentoRequest $request): JsonResponse
    {

        $treinamento = $this->treinamentos->criar($request->only([
            'titulo', 'descricao', 'urlConteudo', 'idTutor',
        ]), $this->principal($request));

        return response()->json((new TreinamentoResource($treinamento))->toArray($request), 201);
    }

    public function avaliar(AvaliarTreinamentoRequest $request, int $id): JsonResponse
    {

        $avaliacao = $this->treinamentos->avaliar($id, $request->only([
            'idFuncionario', 'nota', 'feedback',
        ]), $this->principal($request));

        return response()->json((new AvaliacaoResource($avaliacao))->toArray($request), 201);
    }

    public function avaliacoes(Request $request, int $id): JsonResponse
    {
        return response()->json(collect($this->treinamentos->listarAvaliacoes($id))->map(
            fn ($a) => (new AvaliacaoResource($a))->toArray($request)
        )->all());
    }
}
