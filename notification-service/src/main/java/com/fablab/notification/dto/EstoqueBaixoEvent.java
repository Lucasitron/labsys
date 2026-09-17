package com.fablab.notification.dto;

import java.math.BigDecimal;

/** Evento {@code estoque.baixo.event} consumido do Estoque Service. */
public record EstoqueBaixoEvent(
        Long idItem,
        String nome,
        BigDecimal quantidadeAtual,
        BigDecimal estoqueMinimo) {
}
