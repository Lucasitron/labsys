package com.fablab.notification.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Evento {@code horas.validadas.event} consumido do RH Service (opcional). */
public record HorasValidadasEvent(
        Long idFuncionario,
        String tipo,
        Long idReferencia,
        BigDecimal horas,
        LocalDate data) {
}
