package com.fablab.estoque.dto;

import com.fablab.estoque.entity.TipoSaida;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Resposta de saída de estoque.
 */
public record SaidaResponse(
        Long id,
        Long idItem,
        String nomeItem,
        BigDecimal quantidade,
        TipoSaida tipoSaida,
        Long idReferencia,
        LocalDateTime dataSaida,
        String observacao) {
}