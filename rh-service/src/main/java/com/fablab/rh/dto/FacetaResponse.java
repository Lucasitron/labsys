package com.fablab.rh.dto;

/**
 * Faceta de filtro com contagem (ex.: status "ativo" com 5 pessoas).
 *
 * @param id    identificador do valor (ex.: {@code ativo})
 * @param label rótulo em PT (ex.: {@code Ativo})
 * @param count quantidade de registros com este valor
 */
public record FacetaResponse(
        String id,
        String label,
        long count) {
}
