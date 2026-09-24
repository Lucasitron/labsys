package com.fablab.producao.dto;

import com.fablab.producao.entity.HistoricoUsoMaquina;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Representação de um registro de uso de máquina. */
public record HistoricoUsoResponse(
        Long idUso,
        Long idMaquina,
        Long idFuncionario,
        LocalDateTime dataInicio,
        LocalDateTime dataFim,
        BigDecimal horasUso,
        String observacao) {

    public static HistoricoUsoResponse from(HistoricoUsoMaquina uso) {
        return new HistoricoUsoResponse(
                uso.getIdUso(),
                uso.getMaquina().getIdMaquina(),
                uso.getIdFuncionario(),
                uso.getDataInicio(),
                uso.getDataFim(),
                uso.getHorasUso(),
                uso.getObservacao());
    }
}