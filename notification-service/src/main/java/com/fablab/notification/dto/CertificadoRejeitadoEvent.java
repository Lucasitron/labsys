package com.fablab.notification.dto;

import java.time.LocalDateTime;

/**
 * Evento {@code certificado.rejeitado.event} consumido do RH Service.
 */
public record CertificadoRejeitadoEvent(
        Long idSolicitacao,
        Long idFuncionario,
        String nomeFuncionario,
        String tipoCertificado,
        String observacao,
        LocalDateTime dataDecisao) {
}