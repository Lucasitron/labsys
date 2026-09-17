package com.fablab.financeiro.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Relatório de fluxo de caixa em um período.
 */
public record FluxoCaixaResponse(
        LocalDate dataInicio,
        LocalDate dataFim,
        BigDecimal totalEntradas,
        BigDecimal totalSaidas,
        BigDecimal saldo) {

    public static FluxoCaixaResponse of(LocalDate inicio, LocalDate fim, BigDecimal entradas,
                                        BigDecimal saidas) {
        return new FluxoCaixaResponse(inicio, fim, entradas, saidas, entradas.subtract(saidas));
    }
}