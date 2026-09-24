package com.fablab.rh.dto;

import java.math.BigDecimal;

/**
 * Total de horas disponíveis (não consolidadas) para solicitar certificado.
 *
 * @param idFuncionario        funcionário autenticado
 * @param totalHorasDisponiveis total de horas válidas ainda não consolidadas
 */
public record HorasDisponiveisResponse(
        Long idFuncionario,
        BigDecimal totalHorasDisponiveis) {
}