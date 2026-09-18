package com.fablab.notification.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Evento {@code certificado.aprovado.event} consumido do RH Service.
 */
public record CertificadoAprovadoEvent(
        Long idSolicitacao,
        Long idCertificado,
        Long idFuncionario,
        String nomeFuncionario,
        String tipoCertificado,
        BigDecimal horasCertificadas,
        LocalDateTime dataEmissao) {
}