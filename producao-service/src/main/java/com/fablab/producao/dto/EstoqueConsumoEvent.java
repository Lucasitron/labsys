package com.fablab.producao.dto;

import java.math.BigDecimal;

/** Evento {@code estoque.consumo.realizado.event} publicado pelo Estoque. */
public record EstoqueConsumoEvent(
        Long idEncomenda,
        Long idItem,
        BigDecimal quantidadeConsumida) {
}