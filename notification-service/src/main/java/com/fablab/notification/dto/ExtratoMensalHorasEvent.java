package com.fablab.notification.dto;

import java.math.BigDecimal;

/**
 * Evento {@code extrato.mensal.horas.event} consumido do RH Service.
 */
public record ExtratoMensalHorasEvent(
        Long idFuncionario,
        String nome,
        String mesReferencia,
        BigDecimal horasPresenca,
        BigDecimal horasEncomenda,
        BigDecimal horasProjeto,
        BigDecimal horasDisponiveis) {
}