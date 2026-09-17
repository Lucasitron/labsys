package com.fablab.financeiro.dto;

import java.math.BigDecimal;

/**
 * Item do relatório de lucratividade por encomenda.
 */
public record LucratividadeItemResponse(
        Long idEncomenda,
        BigDecimal valorVenda,
        BigDecimal custoTotal,
        BigDecimal margemLucro,
        BigDecimal margemPercentual) {

    public static LucratividadeItemResponse of(com.fablab.financeiro.entity.CustoEncomenda custo) {
        BigDecimal percentual = custo.getValorVenda().signum() == 0
                ? BigDecimal.ZERO
                : custo.getMargemLucro()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(custo.getValorVenda(), 2, java.math.RoundingMode.HALF_UP);
        return new LucratividadeItemResponse(
                custo.getIdEncomenda(),
                custo.getValorVenda(),
                custo.getCustoTotal(),
                custo.getMargemLucro(),
                percentual);
    }
}