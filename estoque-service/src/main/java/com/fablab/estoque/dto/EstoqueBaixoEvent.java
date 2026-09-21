package com.fablab.estoque.dto;

import java.math.BigDecimal;

/**
 * Evento {@code estoque.baixo.event}: item abaixo do estoque mínimo.
 */
public record EstoqueBaixoEvent(
        Long idItem,
        String nome,
        BigDecimal quantidadeAtual,
        BigDecimal estoqueMinimo) {
}