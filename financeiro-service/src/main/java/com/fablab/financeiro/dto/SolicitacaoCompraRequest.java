package com.fablab.financeiro.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Payload de registro de solicitação de compra (fluxo informativo).
 */
public record SolicitacaoCompraRequest(
        @NotNull Long idItemEstoque,
        @NotNull @Positive BigDecimal quantidade,
        @NotNull @PositiveOrZero BigDecimal valorEstimado) {
}