package com.fablab.financeiro.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload de fechamento de encomenda (valores congelados após a criação).
 *
 * @param horasEstimadas horas estimadas para a produção
 * @param valorFechado valor fechado com o cliente
 * @param dataFechamento data do fechamento
 */
public record FechamentoEncomendaRequest(
        @NotNull Long idEncomenda,
        @PositiveOrZero BigDecimal horasEstimadas,
        @NotNull @Positive BigDecimal valorFechado,
        @NotNull LocalDate dataFechamento) {
}