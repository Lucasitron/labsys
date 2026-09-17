package com.fablab.financeiro.dto;

import com.fablab.financeiro.entity.DoacaoRecurso;
import com.fablab.financeiro.entity.TipoDoacaoRecurso;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Visão de uma doação ou recurso de projeto.
 */
public record DoacaoRecursoResponse(
        Long idDoacao,
        TipoDoacaoRecurso tipo,
        String origem,
        BigDecimal valor,
        LocalDate dataRecebimento,
        Long idProjetoAssociado) {

    public static DoacaoRecursoResponse of(DoacaoRecurso doacaoRecurso) {
        return new DoacaoRecursoResponse(
                doacaoRecurso.getIdDoacao(),
                doacaoRecurso.getTipo(),
                doacaoRecurso.getOrigem(),
                doacaoRecurso.getValor(),
                doacaoRecurso.getDataRecebimento(),
                doacaoRecurso.getIdProjetoAssociado());
    }
}