package com.fablab.rh.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Totais de horas de um funcionário (presença, encomenda e projeto) e
 * incoerências diárias apontadas (encomenda + projeto maior que presença).
 */
public record FuncionarioHorasResponse(
        Long idFuncionario,
        BigDecimal totalHorasPresenca,
        BigDecimal totalHorasEncomenda,
        BigDecimal totalHorasProjeto,
        List<IncoerenciaHoras> incoerencias) {

    /** Incoerência de coerência diária de horas. */
    public record IncoerenciaHoras(
            LocalDate data,
            BigDecimal horasPresenca,
            BigDecimal horasApontadas) {
    }
}