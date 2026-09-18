package com.fablab.rh.dto;

import com.fablab.rh.entity.TipoCertificado;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Resposta com os dados de um certificado emitido e as horas consolidadas.
 */
public record CertificadoEmitidoResponse(
        Long idCertificado,
        Long idSolicitacao,
        Long idFuncionario,
        String nomeFuncionario,
        TipoCertificado tipoCertificado,
        BigDecimal horasCertificadas,
        LocalDateTime dataEmissao,
        UUID codigoVerificacao,
        List<HoraConsolidadaResponse> horas) {
}