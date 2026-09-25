package com.fablab.producao.dto;

import com.fablab.producao.entity.HistoricoKanban;
import com.fablab.producao.entity.KanbanStatus;
import java.time.LocalDateTime;

/** Registro de movimentação do Kanban. */
public record HistoricoKanbanResponse(
        Long idHistorico,
        Long idEncomenda,
        KanbanStatus statusAnterior,
        KanbanStatus statusNovo,
        LocalDateTime dataAlteracao,
        Long idUsuario,
        String observacao) {

    public static HistoricoKanbanResponse from(HistoricoKanban historico) {
        return new HistoricoKanbanResponse(
                historico.getIdHistorico(),
                historico.getIdEncomenda(),
                historico.getStatusAnterior(),
                historico.getStatusNovo(),
                historico.getDataAlteracao(),
                historico.getIdUsuario(),
                historico.getObservacao());
    }
}