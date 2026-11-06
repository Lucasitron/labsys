<?php

namespace App\Services\Producao;

use App\Enums\Producao\PrioridadeTarefa;
use App\Enums\Producao\TarefaStatus;
use App\Models\Producao\Tarefa;
use App\Policies\ProducaoPolicy;
use App\Shared\Auth\Caller;
use App\Shared\Exceptions\ResourceNotFoundException;
use Illuminate\Support\Facades\DB;

/**
 * Tarefas (F2): vínculo do **projeto** em criar/atualizar/status/remover;
 * `PATCH` próprio usa o vínculo da **tarefa** (compat M8).
 */
class TarefaService
{
    /** @return list<Tarefa> */
    public function listar(?int $idProjeto, ?int $idResponsavel, ?TarefaStatus $status, Caller $principal): array
    {
        ProducaoPolicy::exigeLeitura($principal);

        return Tarefa::query()
            ->when($idProjeto !== null, fn ($q) => $q->where('id_projeto', $idProjeto))
            ->when($idResponsavel !== null, fn ($q) => $q->where('id_responsavel', $idResponsavel))
            ->when($status !== null, fn ($q) => $q->where('status', $status))
            ->orderByDesc('id_tarefa')
            ->get()
            ->all();
    }

    public function detalhar(int $id, Caller $principal): Tarefa
    {
        ProducaoPolicy::exigeLeitura($principal);

        return $this->obter($id);
    }

    public function criar(array $dados, Caller $principal, ProjetoService $projetos): Tarefa
    {
        $projeto = $projetos->obter((int) $dados['idProjeto']);
        ProducaoPolicy::exigeResponsavel(
            $projeto->id_responsavel === null ? null : (int) $projeto->id_responsavel, $principal,
        );

        return DB::transaction(function () use ($dados) {
            return Tarefa::create([
                'id_projeto' => $dados['idProjeto'],
                'titulo' => $dados['titulo'],
                'descricao' => $dados['descricao'] ?? null,
                'id_responsavel' => $dados['idResponsavel'] ?? null,
                'data_inicio' => $dados['dataInicio'] ?? null,
                'data_fim_prevista' => $dados['dataFimPrevista'] ?? null,
                'data_conclusao' => null,
                'status' => TarefaStatus::PENDENTE->value,
                'prioridade' => $dados['prioridade'] ?? PrioridadeTarefa::MEDIA->value,
            ]);
        });
    }

    public function atualizar(int $id, array $dados, Caller $principal, ProjetoService $projetos): Tarefa
    {
        ProducaoPolicy::exigeLeitura($principal);
        $tarefa = $this->obter($id);
        $this->exigeResponsavelProjeto($tarefa, $principal);

        return DB::transaction(function () use ($tarefa, $dados, $projetos) {
            $projetos->obter((int) $dados['idProjeto']);

            $tarefa->update([
                'id_projeto' => $dados['idProjeto'],
                'titulo' => $dados['titulo'],
                'descricao' => $dados['descricao'] ?? null,
                'id_responsavel' => $dados['idResponsavel'] ?? null,
                'data_inicio' => $dados['dataInicio'] ?? null,
                'data_fim_prevista' => $dados['dataFimPrevista'] ?? null,
                'prioridade' => $dados['prioridade'] ?? PrioridadeTarefa::MEDIA->value,
            ]);

            return $tarefa->refresh();
        });
    }

    public function alterarStatus(int $id, TarefaStatus $status, ?string $dataConclusao, Caller $principal): Tarefa
    {
        ProducaoPolicy::exigeLeitura($principal);
        $tarefa = $this->obter($id);
        $this->exigeResponsavelProjeto($tarefa, $principal);

        return DB::transaction(function () use ($tarefa, $status, $dataConclusao) {
            $tarefa->status = $status;
            $tarefa->data_conclusao = $status === TarefaStatus::CONCLUIDA
                ? ($dataConclusao ?? today()->toDateString())
                : $dataConclusao;
            $tarefa->save();

            return $tarefa->refresh();
        });
    }

    /** `PATCH /tarefas/{id}`: conclui a própria tarefa (idempotente). */
    public function concluirPropria(int $id, Caller $principal): Tarefa
    {
        ProducaoPolicy::exigeLeitura($principal);
        $tarefa = $this->obter($id);
        ProducaoPolicy::exigeResponsavel(
            $tarefa->id_responsavel === null ? null : (int) $tarefa->id_responsavel, $principal,
        );

        return DB::transaction(function () use ($tarefa) {
            if ($tarefa->status === TarefaStatus::CONCLUIDA) {
                return $tarefa;
            }

            $tarefa->status = TarefaStatus::CONCLUIDA;
            $tarefa->data_conclusao ??= today()->toDateString();
            $tarefa->save();

            return $tarefa->refresh();
        });
    }

    public function remover(int $id, Caller $principal): void
    {
        ProducaoPolicy::exigeLeitura($principal);
        $tarefa = $this->obter($id);
        $this->exigeResponsavelProjeto($tarefa, $principal);

        $tarefa->delete();
    }

    private function exigeResponsavelProjeto(Tarefa $tarefa, Caller $principal): void
    {
        $tarefa->loadMissing('projeto');

        ProducaoPolicy::exigeResponsavel(
            $tarefa->projeto->id_responsavel === null ? null : (int) $tarefa->projeto->id_responsavel,
            $principal,
        );
    }

    public function obter(int $id): Tarefa
    {
        return Tarefa::find($id)
            ?? throw new ResourceNotFoundException("Tarefa não encontrada: {$id}");
    }
}
