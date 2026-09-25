package com.fablab.producao.dto;

import java.math.BigDecimal;

/** Item consumido em uma produção concluída. */
public record ItemConsumido(
        Long idItem,
        BigDecimal quantidadeConsumida) {
}