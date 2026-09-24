package com.fablab.producao.dto;

import com.fablab.producao.entity.SetorResponsavel;
import java.time.LocalDate;

/** Representação de um responsável de setor. */
public record ResponsavelSetorResponse(
        Long idResponsavel,
        Long idFuncionario,
        LocalDate dataInicio,
        LocalDate dataFim,
        Boolean ativo) {

    public static ResponsavelSetorResponse from(SetorResponsavel responsavel) {
        return new ResponsavelSetorResponse(
                responsavel.getIdResponsavel(),
                responsavel.getIdFuncionario(),
                responsavel.getDataInicio(),
                responsavel.getDataFim(),
                responsavel.getAtivo());
    }
}