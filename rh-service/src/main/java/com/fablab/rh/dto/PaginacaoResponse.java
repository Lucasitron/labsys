package com.fablab.rh.dto;

/**
 * Dados de paginação server-side (página 1-based).
 *
 * @param page       página atual (inicia em 1)
 * @param pageSize   itens por página
 * @param totalItems total de itens filtrados
 * @param totalPages total de páginas
 */
public record PaginacaoResponse(
        int page,
        int pageSize,
        long totalItems,
        int totalPages) {
}
