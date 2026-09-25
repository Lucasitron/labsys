package com.fablab.rh.dto;

import java.util.List;

/**
 * Facetas da listagem de pessoas (distribuição por status, nível e setor).
 *
 * @param statuses lista de contagens por status
 * @param niveis   lista de contagens por nível de acesso
 * @param setores  lista de contagens por departamento/setor
 */
public record FiltrosPessoasResponse(
        List<FacetaResponse> statuses,
        List<FacetaResponse> niveis,
        List<FacetaResponse> setores) {
}
