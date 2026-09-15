package com.fablab.vendas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Item dentro de um orçamento.
 */
public record ItemOrcamentoRequest(
        @NotBlank @Size(max = 300) String descricao,
        @NotNull @Positive BigDecimal quantidade,
        @NotNull @Positive BigDecimal valorUnitario) {
}