package com.fablab.rh.dto;

import com.fablab.rh.entity.Funcionario;

/**
 * Helpers de autorização baseados no principal autenticado.
 */
public final class AutorizacaoHelper {

    private AutorizacaoHelper() {
    }

    /** Se o principal é Admin ou possui o funcionário informado. */
    public static boolean ehAdminOuFuncionario(RhPrincipal principal, Funcionario funcionario) {
        return principal != null
                && (principal.isAdmin()
                || (principal.idFuncionario() != null
                && principal.idFuncionario().equals(funcionario.getId())));
    }
}