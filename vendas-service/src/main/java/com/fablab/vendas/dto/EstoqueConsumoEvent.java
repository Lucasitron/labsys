package com.fablab.vendas.dto;

import java.math.BigDecimal;

/**
 * Evento {@code estoque.consumo.realizado.event} consumido do Estoque Service
 * (fluxo informativo: associa o consumo real ao custo da encomenda).
 */
public record EstoqueConsumoEvent(
        Long idEncomenda,
        Long idItem,
        BigDecimal quantidadeConsumida) {
}