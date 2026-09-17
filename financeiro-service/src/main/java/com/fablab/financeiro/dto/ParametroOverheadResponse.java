package com.fablab.financeiro.dto;

import com.fablab.financeiro.entity.ParametroOverhead;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Visão do parâmetro de overhead vigente.
 */
public record ParametroOverheadResponse(
        Long idParametro,
        BigDecimal valorTaxaHora,
        LocalDate dataVigencia) {

    public static ParametroOverheadResponse of(ParametroOverhead parametro) {
        return new ParametroOverheadResponse(
                parametro.getIdParametro(),
                parametro.getValorTaxaHora(),
                parametro.getDataVigencia());
    }
}