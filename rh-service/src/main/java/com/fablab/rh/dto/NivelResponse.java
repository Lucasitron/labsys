package com.fablab.rh.dto;

/**
 * Nível de acesso com contagem de membros.
 *
 * @param id      identificador (ex.: {@code admin})
 * @param ordem   ordem do nível (0-4)
 * @param rotulo  rótulo em PT (ex.: {@code Admin})
 * @param membros quantidade de funcionários neste nível
 */
public record NivelResponse(
        String id,
        int ordem,
        String rotulo,
        long membros) {
}
