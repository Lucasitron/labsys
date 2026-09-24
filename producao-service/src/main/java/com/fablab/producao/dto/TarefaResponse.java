package com.fablab.producao.dto;

import com.fablab.producao.entity.PrioridadeTarefa;
import com.fablab.producao.entity.Tarefa;
import com.fablab.producao.entity.TarefaStatus;
import java.time.LocalDate;

/** Representação de uma tarefa. */
public record TarefaResponse(
        Long idTarefa,
        Long idProjeto,
        String titulo,
        String descricao,
        Long idResponsavel,
        LocalDate dataInicio,
        LocalDate dataFimPrevista,
        LocalDate dataConclusao,
        TarefaStatus status,
        PrioridadeTarefa prioridade) {

    public static TarefaResponse from(Tarefa tarefa) {
        return new TarefaResponse(
                tarefa.getIdTarefa(),
                tarefa.getProjeto().getIdProjeto(),
                tarefa.getTitulo(),
                tarefa.getDescricao(),
                tarefa.getIdResponsavel(),
                tarefa.getDataInicio(),
                tarefa.getDataFimPrevista(),
                tarefa.getDataConclusao(),
                tarefa.getStatus(),
                tarefa.getPrioridade());
    }
}