package com.fablab.rh.dto;

import com.fablab.rh.entity.TipoCertificado;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Evento emitido quando um funcionário solicita um certificado de horas —
 * consumido pelo Notification Service para avisar o Admin.
 *
 * @param idSolicitacao    id da solicitação criada
 * @param idFuncionario    funcionário que solicitou
 * @param tipoCertificado  tipo do certificado (EXTENSAO, COMPLEMENTAR, ESTAGIO)
 * @param horasSolicitadas horas solicitadas
 * @param dataSolicitacao  data da solicitação
 */
public record CertificadoSolicitadoEvent(
        Long idSolicitacao,
        Long idFuncionario,
        TipoCertificado tipoCertificado,
        BigDecimal horasSolicitadas,
        LocalDateTime dataSolicitacao) {
}