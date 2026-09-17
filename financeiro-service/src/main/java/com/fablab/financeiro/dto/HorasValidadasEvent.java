package com.fablab.financeiro.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Evento {@code horas.validadas.event} consumido do Pessoas &amp; RH Service.
 * Acumula as horas validadas para o custeio por ordem.
 *
 * @param idFuncionario id do funcionário no RH
 * @param tipo tipo do apontamento ({@code ENCOMENDA} ou {@code PROJETO})
 * @param idReferencia id da encomenda/projeto associada
 * @param horas horas validadas
 * @param data data dos apontamentos
 */
public record HorasValidadasEvent(
        Long idFuncionario,
        String tipo,
        Long idReferencia,
        BigDecimal horas,
        LocalDate data) {
}