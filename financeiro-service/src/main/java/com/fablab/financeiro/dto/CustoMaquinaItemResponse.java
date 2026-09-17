package com.fablab.financeiro.dto;

import java.math.BigDecimal;

/**
 * Item do relatório de custo por máquina (agrupado pela referência externa dos
 * lançamentos de saída que identificam a máquina).
 */
public record CustoMaquinaItemResponse(
        String referenciaExterna,
        BigDecimal custoTotal) {
}