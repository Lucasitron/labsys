package com.fablab.rh.dto;

import com.fablab.rh.entity.TipoCertificado;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Solicitação de certificado de horas por um funcionário.
 *
 * @param tipoCertificado    tipo do certificado
 * @param horasSolicitadas   horas solicitadas (não podem exceder as disponíveis)
 */
public record SolicitarCertificadoRequest(
        @NotNull(message = "tipoCertificado é obrigatório")
        TipoCertificado tipoCertificado,

        @NotNull(message = "horasSolicitadas é obrigatório")
        @DecimalMin(value = "0.01", message = "horasSolicitadas deve ser maior que zero")
        @DecimalMax(value = "9999.99", message = "horasSolicitadas deve ser no máximo 9999,99")
        BigDecimal horasSolicitadas) {
}