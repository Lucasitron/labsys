package com.fablab.vendas.dto;

import com.fablab.vendas.entity.Prioridade;
import com.fablab.vendas.entity.StatusTarefa;
import com.fablab.vendas.entity.TarefaMarketing;
import java.time.LocalDate;

/**
 * Resposta de tarefa de marketing.
 */
public record TarefaResponse(
        Long id,
        String titulo,
        String descricao,
        Long idResponsavel,
        LocalDate dataInicio,
        LocalDate dataFim,
        StatusTarefa status,
        Prioridade prioridade) {

    public static TarefaResponse of(TarefaMarketing tarefa) {
        return new TarefaResponse(
                tarefa.getId(),
                tarefa.getTitulo(),
                tarefa.getDescricao(),
                tarefa.getIdResponsavel(),
                tarefa.getDataInicio(),
                tarefa.getDataFim(),
                tarefa.getStatus(),
                tarefa.getPrioridade());
    }
}