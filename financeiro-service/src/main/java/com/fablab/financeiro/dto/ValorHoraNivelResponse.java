package com.fablab.financeiro.dto;

import com.fablab.financeiro.entity.ValorHoraNivel;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Visão do valor da hora por nível de acesso.
 */
public record ValorHoraNivelResponse(
        Long idValorHora,
        Integer nivelAcesso,
        BigDecimal valorHora,
        LocalDate dataVigencia) {

    public static ValorHoraNivelResponse of(ValorHoraNivel valorHoraNivel) {
        return new ValorHoraNivelResponse(
                valorHoraNivel.getIdValorHora(),
                valorHoraNivel.getNivelAcesso(),
                valorHoraNivel.getValorHora(),
                valorHoraNivel.getDataVigencia());
    }
}