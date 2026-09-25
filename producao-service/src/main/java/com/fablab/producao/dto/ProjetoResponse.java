package com.fablab.producao.dto;

import com.fablab.producao.entity.Projeto;
import com.fablab.producao.entity.ProjetoStatus;
import java.time.LocalDate;

/** Representação de um projeto. */
public record ProjetoResponse(
        Long idProjeto,
        String nome,
        String descricao,
        LocalDate dataInicio,
        LocalDate dataFimPrevista,
        LocalDate dataFimReal,
        ProjetoStatus status,
        Long idResponsavel) {

    public static ProjetoResponse from(Projeto projeto) {
        return new ProjetoResponse(
                projeto.getIdProjeto(),
                projeto.getNome(),
                projeto.getDescricao(),
                projeto.getDataInicio(),
                projeto.getDataFimPrevista(),
                projeto.getDataFimReal(),
                projeto.getStatus(),
                projeto.getIdResponsavel());
    }
}