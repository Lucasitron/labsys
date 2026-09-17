package com.fablab.financeiro.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload de definição do parâmetro de overhead (taxa por hora).
 */
public record ParametroOverheadRequest(
        @NotNull @Positive BigDecimal valorTaxaHora,
        @NotNull LocalDate dataVigencia) {
}