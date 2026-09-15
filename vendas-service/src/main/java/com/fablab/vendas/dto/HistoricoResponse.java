package com.fablab.vendas.dto;

import com.fablab.vendas.entity.HistoricoStatusEncomenda;
import com.fablab.vendas.entity.StatusKanban;
import java.time.LocalDateTime;

/**
 * Registro de uma movimentação no Kanban.
 */
public record HistoricoResponse(
        Long id,
        StatusKanban statusAnterior,
        StatusKanban statusNovo,
        LocalDateTime dataAlteracao,
        Long idUsuario,
        String observacao) {

    public static HistoricoResponse of(HistoricoStatusEncomenda historico) {
        return new HistoricoResponse(
                historico.getId(),
                historico.getStatusAnterior(),
                historico.getStatusNovo(),
                historico.getDataAlteracao(),
                historico.getIdUsuario(),
                historico.getObservacao());
    }
}