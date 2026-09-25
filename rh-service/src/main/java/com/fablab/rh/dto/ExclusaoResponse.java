package com.fablab.rh.dto;

/**
 * Confirmação de operação de escrita destrutiva.
 *
 * @param ok indicador de sucesso
 * @param id identificador do recurso afetado
 */
public record ExclusaoResponse(
        boolean ok,
        Long id) {
}
