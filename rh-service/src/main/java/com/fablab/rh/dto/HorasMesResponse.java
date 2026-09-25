package com.fablab.rh.dto;

import java.math.BigDecimal;

/**
 * Horas validadas do mês atual e do mês anterior (base do KPI do detalhe).
 *
 * @param atual    horas validadas no mês corrente (até hoje)
 * @param anterior horas validadas no mês anterior (fechado)
 */
public record HorasMesResponse(
        BigDecimal atual,
        BigDecimal anterior) {
}
