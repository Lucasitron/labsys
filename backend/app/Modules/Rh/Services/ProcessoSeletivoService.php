<?php

namespace App\Modules\Rh\Services;

use App\Modules\Rh\Enums\NivelAcesso;
use App\Modules\Rh\Enums\PessoaStatus;
use App\Modules\Rh\Enums\StatusProcesso;
use App\Modules\Rh\Models\Funcionario;
use App\Modules\Rh\Models\GrupoProcessoSeletivo;
use App\Modules\Rh\Models\Pessoa;
use App\Modules\Rh\Models\ProcessoSeletivo;
use App\Modules\Rh\Models\Tutor;
use App\Modules\Rh\Policies\RhPolicy;
use App\Modules\Rh\RhPrincipal;
use App\Shared\Exceptions\ForbiddenException;
use App\Shared\Exceptions\ResourceNotFoundException;

/**
 * Processo seletivo (F5): candidato = pessoa + funcionário nível 4/Recrutando.
 * Gestão por Admin ou tutor (do processo, quando há dono).
 */
class ProcessoSeletivoService
{
    public function iniciar(array $dados, RhPrincipal $principal): ProcessoSeletivo
    {
        if (Pessoa::where('matricula', $dados['matricula'])->exists()) {
            throw new \InvalidArgumentException("Matrícula já cadastrada: {$dados['matricula']}");
        }

        // Não-admin: resolverTutor já devolve o próprio vínculo (idTutor ignorado).
        $tutor = $this->resolverTutor($dados['idTutor'] ?? null, $principal);

        $candidato = Pessoa::create([
            'nome_completo' => $dados['nomeCompleto'],
            'matricula' => $dados['matricula'],
            'contato' => $dados['contato'] ?? null,
            'turno' => $dados['turno'] ?? null,
            'status' => PessoaStatus::RECRUTANDO,
        ]);

        $funcionario = Funcionario::create([
            'id_pessoa' => (int) $candidato->getKey(),
            'nivel_acesso' => NivelAcesso::RECRUTANDO,
        ]);

        return ProcessoSeletivo::create([
            'id_candidato' => (int) $candidato->getKey(),
            'id_tutor' => (int) $tutor->getKey(),
            'status_processo' => StatusProcesso::INSCRITO,
            'data_inscricao' => today()->toDateString(),
        ]);
    }

    public function atualizarStatus(int $id, StatusProcesso $status, ?string $resultadoFinal, RhPrincipal $principal): ProcessoSeletivo
    {
        $processo = $this->obter($id);
        RhPolicy::exigeTutorOuAdmin($principal, $processo);

        $processo->status_processo = $status;
        if ($resultadoFinal !== null) {
            $processo->resultado_final = $resultadoFinal;
        }
        $processo->save();

        return $processo->refresh();
    }

    /** Kanban por grupos + "Sem grupo" + totais. */
    public function listar(?StatusProcesso $estagio, RhPrincipal $principal): array
    {
        RhPolicy::exigeTutorOuAdmin($principal);

        $grupos = [];
        foreach (GrupoProcessoSeletivo::with('lider.pessoa')->orderBy('id')->get() as $grupo) {
            $membros = $this->membrosDoGrupo((int) $grupo->getKey(), $estagio);
            if ($estagio === null || $membros !== []) {
                $grupos[] = $this->paraGrupo($grupo, $membros);
            }
        }

        $semGrupo = ProcessoSeletivo::with('candidato')
            ->whereNull('id_grupo')
            ->when($estagio !== null, fn ($q) => $q->where('status_processo', $estagio))
            ->orderBy('id')
            ->get()
            ->all();

        if ($semGrupo !== []) {
            $grupos[] = [
                'id' => null, 'nome' => 'Sem grupo',
                'etapa' => $this->etapaPredominante($semGrupo, StatusProcesso::INSCRITO)->value,
                'total' => count($semGrupo), 'idLider' => null, 'nomeLider' => null,
                'candidatos' => $this->paraCandidatos($semGrupo),
            ];
        }

        return [
            'grupos' => $grupos,
            'totais' => [
                'processos' => ProcessoSeletivo::count(),
                'grupos' => GrupoProcessoSeletivo::count(),
                'emAvaliacao' => ProcessoSeletivo::whereIn('status_processo', [StatusProcesso::EM_TRIAGEM, StatusProcesso::ENTREVISTA])->count(),
                'aprovados' => ProcessoSeletivo::where('status_processo', StatusProcesso::APROVADO)->count(),
            ],
        ];
    }

    /** Cria grupo reunindo candidatos com processo aberto; líder deve ser tutor. */
    public function criarGrupo(array $dados, RhPrincipal $principal): array
    {
        RhPolicy::exigeTutorOuAdmin($principal);

        $lider = Funcionario::find($dados['idLider'])
            ?? throw new ResourceNotFoundException("Líder não encontrado: {$dados['idLider']}");

        if (! Tutor::where('id_funcionario', $lider->getKey())->exists()) {
            throw new \InvalidArgumentException("Líder do grupo deve ser tutor: {$dados['idLider']}");
        }

        $grupo = GrupoProcessoSeletivo::create([
            'nome' => $dados['nome'],
            'id_tutor_lider' => (int) $lider->getKey(),
            'etapa' => StatusProcesso::INSCRITO,
        ]);

        foreach ($dados['membroIds'] ?? [] as $pessoaId) {
            $processo = ProcessoSeletivo::where('id_candidato', $pessoaId)->first()
                ?? throw new ResourceNotFoundException("Candidato sem processo seletivo: {$pessoaId}");
            $processo->id_grupo = (int) $grupo->getKey();
            $processo->save();
        }

        return $this->paraGrupo($grupo->load('lider.pessoa'), $this->membrosDoGrupo((int) $grupo->getKey(), null));
    }

    public function moverEstagio(int $id, StatusProcesso $etapa, RhPrincipal $principal): ProcessoSeletivo
    {
        $processo = $this->obter($id);
        RhPolicy::exigeTutorOuAdmin($principal, $processo);

        $processo->status_processo = $etapa;
        $processo->save();

        if ($processo->id_grupo !== null) {
            $grupo = GrupoProcessoSeletivo::find($processo->id_grupo);
            if ($grupo !== null) {
                $grupo->etapa = $this->etapaPredominante($this->membrosDoGrupo((int) $grupo->getKey(), null), $grupo->etapa);
                $grupo->save();
            }
        }

        return $processo->refresh();
    }

    public function avaliar(int $id, int $pessoaId, float $nota, ?string $feedback, RhPrincipal $principal): ProcessoSeletivo
    {
        $processo = $this->obter($id);

        if ((int) $processo->id_candidato !== $pessoaId) {
            throw new ResourceNotFoundException("Candidato {$pessoaId} não pertence ao processo {$id}");
        }

        RhPolicy::exigeTutorOuAdmin($principal, $processo);

        $processo->nota = number_format($nota, 2, '.', '');
        $processo->feedback = $feedback;
        $processo->save();

        return $processo->refresh();
    }

    public function obter(int $id): ProcessoSeletivo
    {
        return ProcessoSeletivo::find($id)
            ?? throw new ResourceNotFoundException("Processo seletivo não encontrado: {$id}");
    }

    private function resolverTutor(?int $idTutor, RhPrincipal $principal): Funcionario
    {
        if ($principal->isAdmin()) {
            if ($idTutor === null) {
                throw new \InvalidArgumentException('idTutor é obrigatório quando criado por Admin');
            }

            return Funcionario::find($idTutor)
                ?? throw new ResourceNotFoundException("Tutor não encontrado: {$idTutor}");
        }

        if ($principal->idFuncionario === null || ! RhPolicy::ehTutor($principal)) {
            throw new ForbiddenException('Apenas Admin ou tutor podem iniciar um processo seletivo');
        }

        return Funcionario::find($principal->idFuncionario)
            ?? throw new ForbiddenException('Funcionário não encontrado');
    }

    /** @return list<ProcessoSeletivo> */
    private function membrosDoGrupo(int $idGrupo, ?StatusProcesso $estagio): array
    {
        return ProcessoSeletivo::with('candidato')
            ->where('id_grupo', $idGrupo)
            ->when($estagio !== null, fn ($q) => $q->where('status_processo', $estagio))
            ->orderBy('id')
            ->get()
            ->all();
    }

    /** @param list<ProcessoSeletivo> $membros */
    private function paraGrupo(GrupoProcessoSeletivo $grupo, array $membros): array
    {
        return [
            'id' => (int) $grupo->getKey(),
            'nome' => $grupo->nome,
            'etapa' => $this->etapaPredominante($membros, $grupo->etapa)->value,
            'total' => count($membros),
            'idLider' => (int) $grupo->id_tutor_lider,
            'nomeLider' => $grupo->lider->pessoa->nome_completo,
            'candidatos' => $this->paraCandidatos($membros),
        ];
    }

    /** @param list<ProcessoSeletivo> $processos */
    private function paraCandidatos(array $processos): array
    {
        return array_map(fn (ProcessoSeletivo $p) => [
            'idPessoa' => (int) $p->id_candidato,
            'nome' => $p->candidato->nome_completo,
            'status' => $p->status_processo->value,
            'nota' => $p->nota !== null ? number_format((float) $p->nota, 2, '.', '') : null,
            'feedback' => $p->feedback,
        ], $processos);
    }

    /** @param list<ProcessoSeletivo> $membros */
    private function etapaPredominante(array $membros, StatusProcesso $padrao): StatusProcesso
    {
        if ($membros === []) {
            return $padrao;
        }

        $contagem = [];
        foreach ($membros as $m) {
            $contagem[$m->status_processo->value] = ($contagem[$m->status_processo->value] ?? 0) + 1;
        }
        arsort($contagem);

        return StatusProcesso::from(array_key_first($contagem));
    }
}
