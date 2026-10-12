<?php

namespace App\Modules\Rh\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Rh\Enums\StatusProcesso;
use App\Modules\Rh\Http\Requests\AtualizarStatusProcessoRequest;
use App\Modules\Rh\Http\Requests\AvaliarCandidatoRequest;
use App\Modules\Rh\Http\Requests\CriarGrupoRequest;
use App\Modules\Rh\Http\Requests\IniciarProcessoRequest;
use App\Modules\Rh\Http\Requests\MoverEstagioRequest;
use App\Modules\Rh\Http\Resources\ProcessoResource;
use App\Modules\Rh\RhPrincipal;
use App\Modules\Rh\Services\ProcessoSeletivoService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Processo seletivo — só HTTP (gestão por Admin ou tutor, no service). */
class ProcessoSeletivoController
{
    public function __construct(private ProcessoSeletivoService $processos) {}

    public function store(IniciarProcessoRequest $request): JsonResponse
    {
        /** @var Login $login */
        $login = $request->user();

        $processo = $this->processos->iniciar($request->only([
            'nomeCompleto', 'matricula', 'contato', 'turno', 'idTutor',
        ]), RhPrincipal::from($login));

        return response()->json(
            (new ProcessoResource($processo->load('candidato')))->toArray($request),
            201
        );
    }

    public function update(AtualizarStatusProcessoRequest $request, int $id): JsonResponse
    {
        /** @var Login $login */
        $login = $request->user();

        $processo = $this->processos->atualizarStatus(
            $id,
            StatusProcesso::from($request->input('statusProcesso')),
            $request->input('resultadoFinal'),
            RhPrincipal::from($login),
        )->load('candidato');

        return response()->json((new ProcessoResource($processo))->toArray($request));
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'estagio' => ['nullable', 'in:INSCRITO,EM_TRIAGEM,ENTREVISTA,APROVADO,REPROVADO'],
        ]);

        /** @var Login $login */
        $login = $request->user();

        return response()->json($this->processos->listar(
            isset($filtros['estagio']) ? StatusProcesso::from($filtros['estagio']) : null,
            RhPrincipal::from($login),
        ));
    }

    public function criarGrupo(CriarGrupoRequest $request): JsonResponse
    {
        /** @var Login $login */
        $login = $request->user();

        return response()->json($this->processos->criarGrupo($request->only([
            'nome', 'idLider', 'membroIds',
        ]), RhPrincipal::from($login)), 201);
    }

    public function moverEstagio(MoverEstagioRequest $request, int $id): JsonResponse
    {
        /** @var Login $login */
        $login = $request->user();

        $processo = $this->processos->moverEstagio(
            $id,
            StatusProcesso::from($request->input('etapa')),
            RhPrincipal::from($login),
        )->load('candidato');

        return response()->json((new ProcessoResource($processo))->toArray($request));
    }

    public function avaliar(AvaliarCandidatoRequest $request, int $id, int $pessoaId): JsonResponse
    {
        /** @var Login $login */
        $login = $request->user();

        $processo = $this->processos->avaliar(
            $id, $pessoaId, (float) $request->input('nota'), $request->input('feedback'), RhPrincipal::from($login),
        );

        return response()->json(
            (new ProcessoResource($processo->load('candidato')))->toArray($request),
            201
        );
    }
}
