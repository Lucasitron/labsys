<?php

namespace App\Contracts;

use App\Enums\Producao\KanbanStatus;
use App\Enums\Producao\MaquinaStatus;
use App\Enums\Producao\TarefaStatus;
use App\Models\Producao\EncomendaKanban;
use App\Models\Producao\Maquina;
use App\Models\Producao\Tarefa;

class DefaultProducaoContract implements ProducaoContract
{
    public function resumoKanban(): array
    {
        $resumo = [];
        foreach (KanbanStatus::cases() as $status) {
            $resumo[$status->value] = EncomendaKanban::where('status', $status)->count();
        }

        return $resumo;
    }

    public function tarefasAtivasPorResponsavel(int $idResponsavel): array
    {
        return Tarefa::where('id_responsavel', $idResponsavel)
            ->where('status', '!=', TarefaStatus::CONCLUIDA)
            ->orderBy('id_tarefa')
            ->get()
            ->map(fn (Tarefa $tarefa) => [
                'id' => (int) $tarefa->getKey(),
                'idProjeto' => (int) $tarefa->id_projeto,
                'titulo' => $tarefa->titulo,
                'status' => $tarefa->status->value,
                'prioridade' => $tarefa->prioridade->value,
                'due' => $tarefa->data_fim_prevista?->toDateString(),
            ])
            ->all();
    }

    public function resumoMaquinas(): array
    {
        $resumo = [];
        foreach (MaquinaStatus::cases() as $status) {
            $resumo[$status->value] = Maquina::where('status', $status)->count();
        }

        return $resumo;
    }
}
