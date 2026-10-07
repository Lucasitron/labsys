<?php

namespace App\Modules\Dashboard\Policies;

use App\Modules\Auth\Enums\Role;
use App\Shared\Exceptions\ForbiddenException;

/**
 * Autorização do Dashboard (rbac-matrix.md linha Dashboard).
 *
 * - Resumo/KPIs: roles 0–3 (Admin pleno, demais sem as chaves `restricted`);
 *   Recrutando (4) ou sem papel ativo → 403.
 * - Concluir tarefa (`PATCH /api/tasks/{id}`) reusa M6 (`concluirPropria` +
 *   `exigeLeitura`) — sem policy nova.
 */
class DashboardPolicy
{
    /** Resumo liberado p/ 0–3 (matriz regra 6: `restricted` filtrado no service). */
    public static function podeVerResumo(?Role $role): bool
    {
        return $role !== null && $role !== Role::RECRUTANDO;
    }

    /** @throws ForbiddenException */
    public static function exigeResumo(?Role $role): void
    {
        if (! self::podeVerResumo($role)) {
            throw new ForbiddenException('Acesso negado ao dashboard');
        }
    }
}
