package com.fablab.rh.dto;

import java.math.BigDecimal;

/**
 * Resumo de treinamentos do integrante (aba Treinamentos do detalhe).
 *
 * @param concluidos quantidade de avaliações registradas
 * @param media      nota média (nula quando não há avaliações)
 */
public record TreinamentosResumoResponse(
        long concluidos,
        BigDecimal media) {
}
