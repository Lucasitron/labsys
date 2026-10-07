<?php

namespace App\Modules\Producao\Contracts;

use App\Modules\Producao\Enums\KanbanStatus;
use App\Modules\Producao\Enums\TarefaStatus;
use App\Modules\Producao\Models\EncomendaKanban;
use App\Modules\Producao\Models\Tarefa;

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
            ])
            ->all();
    }
}
