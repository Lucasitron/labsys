<?php

namespace App\Modules\Producao\Contracts;

/**
 * Fronteira pública do Producao p/ Dashboard (M8, agregador in-process, sem
 * HTTP). Congelado em 2 métodos — só o consumido.
 */
interface ProducaoContract
{
    /** Contagem de cartões por status do Kanban (`status => total`). */
    public function resumoKanban(): array;

    /** Tarefas não-concluídas do responsável (p/ `PATCH /api/tasks/{id}` em M8). */
    public function tarefasAtivasPorResponsavel(int $idResponsavel): array;
}
