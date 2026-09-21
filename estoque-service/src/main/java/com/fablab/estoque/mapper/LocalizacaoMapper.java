package com.fablab.estoque.mapper;

import com.fablab.estoque.dto.LocalizacaoResponse;
import com.fablab.estoque.entity.Localizacao;

/**
 * Mapeia entidades {@code Localizacao} para DTOs.
 */
public final class LocalizacaoMapper {

    private LocalizacaoMapper() {
    }

    public static LocalizacaoResponse toResponse(Localizacao localizacao) {
        if (localizacao == null) {
            return null;
        }
        return new LocalizacaoResponse(
                localizacao.getId(),
                localizacao.getArmario(),
                localizacao.getPrateleira(),
                localizacao.getCaixa(),
                localizacao.getDescricao());
    }
}