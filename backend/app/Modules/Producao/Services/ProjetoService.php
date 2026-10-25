<?php

namespace App\Modules\Producao\Services;

use App\Modules\Producao\Enums\ProjetoStatus;
use App\Modules\Producao\Models\Projeto;
use App\Modules\Producao\Policies\ProducaoPolicy;
use App\Modules\Producao\ProducaoPrincipal;
use App\Shared\Exceptions\ResourceNotFoundException;
use Illuminate\Support\Facades\DB;

/** Projetos (F1): criar-só-se-responsável, status com carimbo, vínculo em tudo. */
class ProjetoService
{
    /** @return list<Projeto> */
    public function listar(?ProjetoStatus $status, ?int $idResponsavel, ProducaoPrincipal $principal): array
    {
        ProducaoPolicy::exigeLeitura($principal);

        return Projeto::query()
            ->when($status !== null, fn ($q) => $q->where('status', $status))
            ->when($idResponsavel !== null, fn ($q) => $q->where('id_responsavel', $idResponsavel))
            ->orderByDesc('id_projeto')
            ->get()
            ->all();
    }

    public function detalhar(int $id, ProducaoPrincipal $principal): Projeto
    {
        ProducaoPolicy::exigeLeitura($principal);

        return $this->obter($id);
    }

    public function criar(array $dados, ProducaoPrincipal $principal): Projeto
    {
        ProducaoPolicy::exigeResponsavel((int) $dados['idResponsavel'], $principal);

        return DB::transaction(function () use ($dados) {
            return Projeto::create([
                'nome' => $dados['nome'],
                'descricao' => $dados['descricao'] ?? null,
                'data_inicio' => $dados['dataInicio'],
                'data_fim_prevista' => $dados['dataFimPrevista'] ?? null,
                'data_fim_real' => null,
                'status' => $dados['status'] ?? ProjetoStatus::PLANEJADO->value,
                'id_responsavel' => $dados['idResponsavel'],
            ]);
        });
    }

    public function atualizar(int $id, array $dados, ProducaoPrincipal $principal): Projeto
    {
        // 403 antes de 404: sem papel no módulo, nem a existência vaza.
        ProducaoPolicy::exigeLeitura($principal);
        $projeto = $this->obter($id);
        ProducaoPolicy::exigeResponsavel(
            $projeto->id_responsavel === null ? null : (int) $projeto->id_responsavel, $principal,
        );

        return DB::transaction(function () use ($projeto, $dados) {
            $projeto->update([
                'nome' => $dados['nome'],
                'descricao' => $dados['descricao'] ?? null,
                'data_inicio' => $dados['dataInicio'],
                'data_fim_prevista' => $dados['dataFimPrevista'] ?? null,
                'id_responsavel' => $dados['idResponsavel'],
            ]);

            return $projeto->refresh();
        });
    }

    public function alterarStatus(int $id, ProjetoStatus $status, ?string $dataFimReal, ProducaoPrincipal $principal): Projeto
    {
        ProducaoPolicy::exigeLeitura($principal);
        $projeto = $this->obter($id);
        ProducaoPolicy::exigeResponsavel(
            $projeto->id_responsavel === null ? null : (int) $projeto->id_responsavel, $principal,
        );

        return DB::transaction(function () use ($projeto, $status, $dataFimReal) {
            $projeto->status = $status;
            $projeto->data_fim_real = $status === ProjetoStatus::CONCLUIDO
                ? ($dataFimReal ?? today()->toDateString())
                : $dataFimReal;
            $projeto->save();

            return $projeto->refresh();
        });
    }

    public function remover(int $id, ProducaoPrincipal $principal): void
    {
        ProducaoPolicy::exigeLeitura($principal);
        $projeto = $this->obter($id);
        ProducaoPolicy::exigeResponsavel(
            $projeto->id_responsavel === null ? null : (int) $projeto->id_responsavel, $principal,
        );

        $projeto->delete();
    }

    public function obter(int $id): Projeto
    {
        return Projeto::find($id)
            ?? throw new ResourceNotFoundException("Projeto não encontrado: {$id}");
    }
}
