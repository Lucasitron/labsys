package com.fablab.rh.dto;

import com.fablab.rh.entity.TipoCertificado;
import java.time.LocalDateTime;

/**
 * Evento emitido quando uma solicitação de certificado é rejeitada — consumido
 * pelo Notification Service para avisar o funcionário.
 *
 * @param idSolicitacao    solicitação rejeitada
 * @param idFuncionario    funcionário solicitante
 * @param nomeFuncionario  nome do funcionário (para o e-mail)
 * @param tipoCertificado  tipo do certificado
 * @param observacao       motivo/observação do Admin
 * @param dataDecisao      data da rejeição
 */
public record CertificadoRejeitadoEvent(
        Long idSolicitacao,
        Long idFuncionario,
        String nomeFuncionario,
        TipoCertificado tipoCertificado,
        String observacao,
        LocalDateTime dataDecisao) {
}