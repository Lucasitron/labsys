package com.fablab.estoque.dto;

import java.math.BigDecimal;

/**
 * Item consumido em um evento de produção concluída.
 */
public record ItemConsumido(
        Long idItem,
        BigDecimal quantidadeConsumida) {
}