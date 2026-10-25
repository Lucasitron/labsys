<?php

namespace App\Modules\Estoque\Policies;

use App\Modules\Auth\Enums\Role;
use App\Modules\Estoque\EstoquePrincipal;
use App\Shared\Exceptions\ForbiddenException;

/**
 * Autorização do Estoque (port de EDICAO/VISUALIZACAO/OPERACAO + rbac-matrix.md,
 * que prevalece onde diverge do Java — plan §M3.1.4).
 *
 * - Leitura: qualquer autenticado, exceto Recrutando (matriz: `–` no módulo).
 * - Escrita itens/entradas/saídas/empréstimos: Admin ou Bolsista/Voluntário com
 *   vínculo de estoque (matriz: "Editar (se responsável)").
 * - Escrita BOM/consumo: Admin ou Bolsista com vínculo (matriz: Voluntário só
 *   visualiza a BOM).
 * - Escrita fornecedores/localizações: só Admin (matriz: demais visualizam).
 */
class EstoquePolicy
{
    /** @throws ForbiddenException */
    public static function exigeLeitura(EstoquePrincipal $principal): void
    {
        if ($principal->role === Role::RECRUTANDO) {
            throw new ForbiddenException('Acesso negado ao módulo de estoque');
        }
    }

    public static function podeEditarMovimentacao(EstoquePrincipal $principal): bool
    {
        return $principal->isAdmin()
            || (($principal->role === Role::BOLSISTA || $principal->role === Role::VOLUNTARIO)
                && $principal->responsavelEstoque);
    }

    /** @throws ForbiddenException */
    public static function exigeEdicaoMovimentacao(EstoquePrincipal $principal): void
    {
        self::exigeLeitura($principal);

        if (! self::podeEditarMovimentacao($principal)) {
            throw new ForbiddenException('Apenas Admin ou responsável pelo estoque pode alterar este recurso');
        }
    }

    public static function podeEditarBom(EstoquePrincipal $principal): bool
    {
        return $principal->isAdmin()
            || ($principal->role === Role::BOLSISTA && $principal->responsavelEstoque);
    }

    /** @throws ForbiddenException */
    public static function exigeEdicaoBom(EstoquePrincipal $principal): void
    {
        self::exigeLeitura($principal);

        if (! self::podeEditarBom($principal)) {
            throw new ForbiddenException('Apenas Admin ou Bolsista responsável pelo estoque pode alterar a BOM');
        }
    }

    /** E-7: Admin vê todos os empréstimos; demais, só os próprios. */
    public static function canSeeLoans(EstoquePrincipal $principal, int $idPessoa): bool
    {
        return $principal->isAdmin() || $principal->idPessoa === $idPessoa;
    }

    /** @throws ForbiddenException */
    public static function exigeAcessoEmprestimo(EstoquePrincipal $principal, int $idPessoa): void
    {
        if (! self::canSeeLoans($principal, $idPessoa)) {
            throw new ForbiddenException('Apenas o Admin ou o responsável pelo empréstimo pode consultar este empréstimo');
        }
    }
}
