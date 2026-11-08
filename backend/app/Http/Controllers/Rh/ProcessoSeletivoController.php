<?php

namespace App\Http\Controllers\Rh;

use App\Http\Controllers\Controller;
use App\Http\Controllers\Rh\ResolvesRhPrincipal;

use App\Enums\Rh\StatusProcesso;
use App\Http\Requests\Rh\AtualizarStatusProcessoRequest;
use App\Http\Requests\Rh\AvaliarCandidatoRequest;
use App\Http\Requests\Rh\CriarGrupoRequest;
use App\Http\Requests\Rh\IniciarProcessoRequest;
use App\Http\Requests\Rh\MoverEstagioRequest;
use App\Http\Resources\Rh\ProcessoResource;
use App\Services\Rh\ProcessoSeletivoService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Processo seletivo — só HTTP (gestão por Admin ou tutor, no service). */
class ProcessoSeletivoController extends Controller
{
    use ResolvesRhPrincipal;
    public function __construct(private ProcessoSeletivoService $processos) {}

    public function store(IniciarProcessoRequest $request): JsonResponse
    {

        $processo = $this->processos->iniciar($request->only([
            'nomeCompleto', 'matricula', 'contato', 'turno', 'idTutor',
        ]), $this->principal($request));

        return response()->json(
            (new ProcessoResource($processo->load('candidato')))->toArray($request),
            201
        );
    }

    public function update(AtualizarStatusProcessoRequest $request, int $id): JsonResponse
    {

        $processo = $this->processos->atualizarStatus(
            $id,
            StatusProcesso::from($request->input('statusProcesso')),
            $request->input('resultadoFinal'),
            $this->principal($request),
        )->load('candidato');

        return response()->json((new ProcessoResource($processo))->toArray($request));
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'estagio' => ['nullable', 'in:INSCRITO,EM_TRIAGEM,ENTREVISTA,APROVADO,REPROVADO'],
        ]);


        return response()->json($this->processos->listar(
            isset($filtros['estagio']) ? StatusProcesso::from($filtros['estagio']) : null,
            $this->principal($request),
        ));
    }

    public function criarGrupo(CriarGrupoRequest $request): JsonResponse
    {

        return response()->json($this->processos->criarGrupo($request->only([
            'nome', 'idLider', 'membroIds',
        ]), $this->principal($request)), 201);
    }

    public function moverEstagio(MoverEstagioRequest $request, int $id): JsonResponse
    {

        $processo = $this->processos->moverEstagio(
            $id,
            StatusProcesso::from($request->input('etapa')),
            $this->principal($request),
        )->load('candidato');

        return response()->json((new ProcessoResource($processo))->toArray($request));
    }

    public function avaliar(AvaliarCandidatoRequest $request, int $id, int $pessoaId): JsonResponse
    {

        $processo = $this->processos->avaliar(
            $id, $pessoaId, (float) $request->input('nota'), $request->input('feedback'), $this->principal($request),
        );

        return response()->json(
            (new ProcessoResource($processo->load('candidato')))->toArray($request),
            201
        );
    }
}
