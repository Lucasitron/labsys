<?php

namespace App\Modules\Producao\Contracts;

/**
 * Fronteira pública do Producao p/ Dashboard (M8, agregador in-process, sem
 * HTTP). 3 métodos — só o consumido.
 */
interface ProducaoContract
{
    /** Contagem de cartões por status do Kanban (`status => total`). */
    public function resumoKanban(): array;

    /**
     * Tarefas não-concluídas do responsável (p/ `PATCH /api/tasks/{id}` em M8).
     * Cada row traz `due` (`data_fim_prevista` Y-m-d ou null) p/ o dashboard.
     */
    public function tarefasAtivasPorResponsavel(int $idResponsavel): array;

    /** Contagem de máquinas por status (`status => total`, espelho de `resumoKanban`). */
    public function resumoMaquinas(): array;
}
