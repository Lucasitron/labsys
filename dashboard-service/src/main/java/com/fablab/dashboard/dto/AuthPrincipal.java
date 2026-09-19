package com.fablab.dashboard.dto;

/**
 * Principal autenticado, extraído do JWT emitido pelo Auth Service.
 *
 * @param idUser   id do usuário (claim {@code id_user})
 * @param username usuário de acesso (subject do token)
 * @param role     nome do nível de acesso (ex.: {@code ADMIN})
 * @param setor    setor informado na claim
 */
public record AuthPrincipal(Long idUser, String username, String role, String setor) {

    /** Se o usuário é Admin (rótulo da role {@code ADMIN}). */
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}