package com.fablab.rh.dto;

import com.fablab.rh.entity.StatusSolicitacao;
import com.fablab.rh.entity.TipoCertificado;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Resposta com os dados de uma solicitação de certificado.
 */
public record SolicitacaoCertificadoResponse(
        Long idSolicitacao,
        Long idFuncionario,
        String nomeFuncionario,
        TipoCertificado tipoCertificado,
        LocalDateTime dataSolicitacao,
        BigDecimal horasSolicitadas,
        StatusSolicitacao status,
        Long idAdminAprovador,
        LocalDateTime dataDecisao,
        String observacao) {
}