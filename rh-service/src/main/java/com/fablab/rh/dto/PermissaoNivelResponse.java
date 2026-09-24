package com.fablab.rh.dto;

/**
 * Capacidade de um nível em um recurso (espelho das regras aplicadas no
 * servidor: {@code TOTAL} = irrestrito, {@code PROPRIO} = só o próprio
 * registro, {@code NAO} = sem acesso).
 *
 * @param id         identificador do recurso (ex.: {@code pessoas})
 * @param rotulo     rótulo em PT
 * @param admin      capacidade do Admin
 * @param bolsista   capacidade do Bolsista
 * @param voluntario capacidade do Voluntário
 * @param estagiario capacidade do Estagiário
 * @param recrutando capacidade do Recrutando
 */
public record PermissaoNivelResponse(
        String id,
        String rotulo,
        String admin,
        String bolsista,
        String voluntario,
        String estagiario,
        String recrutando) {
}
