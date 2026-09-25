package com.fablab.rh.dto;

import com.fablab.rh.entity.TipoCertificado;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Evento emitido quando um certificado de horas é aprovado — consumido pelo
 * Notification Service para avisar o funcionário.
 *
 * @param idSolicitacao      solicitação aprovada
 * @param idCertificado      certificado emitido
 * @param idFuncionario      funcionário beneficiado
 * @param nomeFuncionario    nome do funcionário (para o e-mail)
 * @param tipoCertificado    tipo do certificado
 * @param horasCertificadas  horas certificadas
 * @param dataEmissao        data de emissão
 */
public record CertificadoAprovadoEvent(
        Long idSolicitacao,
        Long idCertificado,
        Long idFuncionario,
        String nomeFuncionario,
        TipoCertificado tipoCertificado,
        BigDecimal horasCertificadas,
        LocalDateTime dataEmissao) {
}