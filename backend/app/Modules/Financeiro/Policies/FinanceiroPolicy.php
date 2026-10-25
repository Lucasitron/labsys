<?php

namespace App\Modules\Financeiro\Policies;

use App\Modules\Financeiro\FinanceiroPrincipal;
use App\Shared\Exceptions\ForbiddenException;

/**
 * Autorização do Financeiro (rbac-matrix.md regra 2 ≡ Java
 * `@PreAuthorize("hasRole('ADMIN')")` — sem delta: módulo 100% Admin).
 * Recrutando (4) e demais papéis não entram; sem escopo "próprio".
 */
class FinanceiroPolicy
{
    /** @throws ForbiddenException */
    public static function exigeAdmin(FinanceiroPrincipal $principal): void
    {
        if (! $principal->isAdmin()) {
            throw new ForbiddenException('Apenas Admin pode acessar o módulo financeiro');
        }
    }
}
