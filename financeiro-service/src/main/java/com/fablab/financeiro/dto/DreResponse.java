package com.fablab.financeiro.dto;

import java.math.BigDecimal;

/**
 * DRE simplificado: receitas, despesas e resultado líquido.
 */
public record DreResponse(
        BigDecimal receitas,
        BigDecimal despesas,
        BigDecimal resultado) {

    public static DreResponse of(BigDecimal receitas, BigDecimal despesas) {
        return new DreResponse(receitas, despesas, receitas.subtract(despesas));
    }
}