package com.fablab.notification.dto;

/**
 * Principal autenticado, extraído do JWT emitido pelo Auth Service.
 *
 * @param idUser   id do usuário (claim {@code id_user})
 * @param username usuário de acesso (subject do token)
 * @param role     nome do nível de acesso (ex.: {@code ADMIN})
 * @param setor    setor informado na claim
 */
public record AuthPrincipal(Long idUser, String username, String role, String setor) {
}