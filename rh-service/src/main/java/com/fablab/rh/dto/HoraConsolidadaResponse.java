package com.fablab.rh.dto;

import com.fablab.rh.entity.TipoApontamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Vínculo de um apontamento consolidado a um certificado emitido.
 */
public record HoraConsolidadaResponse(
        Long idConsolidacao,
        Long idApontamento,
        LocalDate dataApontamento,
        TipoApontamento tipoApontamento,
        BigDecimal horas,
        LocalDateTime dataConsolidacao) {
}