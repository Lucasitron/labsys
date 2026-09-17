package com.fablab.financeiro.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Relatório comparativo de doações/recebimentos versus despesas.
 */
public record DoacoesDespesasResponse(
        LocalDate dataInicio,
        LocalDate dataFim,
        BigDecimal totalDoacoes,
        BigDecimal totalDespesas,
        BigDecimal saldo) {

    public static DoacoesDespesasResponse of(LocalDate inicio, LocalDate fim, BigDecimal doacoes,
                                             BigDecimal despesas) {
        return new DoacoesDespesasResponse(inicio, fim, doacoes, despesas, doacoes.subtract(despesas));
    }
}