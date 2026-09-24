package com.fablab.rh.dto;

import java.util.List;

/**
 * Página de pessoas com paginação server-side e facetas de filtro.
 *
 * @param pessoas   pessoas da página atual
 * @param paginacao dados de paginação
 * @param filtros   facetas com contagens (status, níveis, setores)
 */
public record PessoasPaginaResponse(
        List<PessoaResponse> pessoas,
        PaginacaoResponse paginacao,
        FiltrosPessoasResponse filtros) {
}
