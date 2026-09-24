package com.fablab.rh.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Evento emitido quando apontamentos de horas são validados — consumido pelo
 * Financeiro Service para cálculo de custo e coerência.
 *
 * @param idFuncionario    id do funcionário
 * @param tipo             tipo do apontamento (ENCOMENDA, PROJETO)
 * @param idReferencia     referência da encomenda/projeto
 * @param horas            horas validadas
 * @param data             data dos apontamentos
 */
public record HorasValidadasEvent(
        Long idFuncionario,
        String tipo,
        Long idReferencia,
        BigDecimal horas,
        LocalDate data) {
}