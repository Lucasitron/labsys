<?php

namespace App\Modules\Vendas\Policies;

use App\Modules\Auth\Enums\Role;
use App\Modules\Vendas\VendasPrincipal;
use App\Shared\Exceptions\ForbiddenException;

/**
 * Autorização do Vendas (rbac-matrix.md prevalece sobre o Java — plan §M4.1.4).
 *
 * - Leitura: qualquer autenticado, exceto Recrutando (matriz: `–` no módulo).
 * - Escrita de domínio: só Admin (matriz: demais só visualizam; sem papel
 *   "responsável de Vendas" por decisão do PO).
 * - Kanban mover + nova-ordem: criador (`criado_por` = claim `id_user`) ou
 *   Admin, senão 403 (PermissaoUtil.exigirCriadorOuAdmin preservado).
 * - Solicitar (POST): qualquer autenticado exceto Recrutando (pedido, não
 *   decisão); decidir + bulk-tag = Admin-only.
 */
class VendasPolicy
{
    /** @throws ForbiddenException */
    public static function exigeLeitura(VendasPrincipal $principal): void
    {
        if ($principal->role === Role::RECRUTANDO) {
            throw new ForbiddenException('Acesso negado ao módulo de vendas');
        }
    }

    /** @throws ForbiddenException */
    public static function exigeEscrita(VendasPrincipal $principal): void
    {
        self::exigeLeitura($principal);

        if (! $principal->isAdmin()) {
            throw new ForbiddenException('Apenas Admin pode alterar este recurso');
        }
    }

    /** @throws ForbiddenException */
    public static function exigirCriadorOuAdmin(?int $criadoPor, VendasPrincipal $principal): void
    {
        self::exigeLeitura($principal);

        if ($principal->isAdmin()) {
            return;
        }

        if ($criadoPor === null || $criadoPor !== $principal->idPessoa) {
            throw new ForbiddenException('Apenas quem criou o registro ou Admin pode alterar. Use Solicitar alteração.');
        }
    }
}
