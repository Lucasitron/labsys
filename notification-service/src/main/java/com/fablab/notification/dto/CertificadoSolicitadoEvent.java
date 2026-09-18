package com.fablab.notification.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Evento {@code certificado.solicitado.event} consumido do RH Service.
 */
public record CertificadoSolicitadoEvent(
        Long idSolicitacao,
        Long idFuncionario,
        String tipoCertificado,
        BigDecimal horasSolicitadas,
        LocalDateTime dataSolicitacao) {
}