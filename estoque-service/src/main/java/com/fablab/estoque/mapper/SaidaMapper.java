package com.fablab.estoque.mapper;

import com.fablab.estoque.dto.SaidaResponse;
import com.fablab.estoque.entity.SaidaEstoque;

/**
 * Mapeia entidades {@code SaidaEstoque} para DTOs.
 */
public final class SaidaMapper {

    private SaidaMapper() {
    }

    public static SaidaResponse toResponse(SaidaEstoque saida) {
        return new SaidaResponse(
                saida.getId(),
                saida.getItem().getId(),
                saida.getItem().getNome(),
                saida.getQuantidade(),
                saida.getTipoSaida(),
                saida.getIdReferencia(),
                saida.getDataSaida().withNano(0),
                saida.getObservacao(),
                saida.getResponsavel());
    }
}