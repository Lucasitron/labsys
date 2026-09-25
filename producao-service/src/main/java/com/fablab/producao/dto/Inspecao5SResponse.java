package com.fablab.producao.dto;

import com.fablab.producao.entity.Inspecao5S;
import com.fablab.producao.entity.StatusInspecao;
import com.fablab.producao.entity.TurnoInspecao;
import java.time.LocalDate;
import java.util.List;

/** Representação de uma inspeção 5S. */
public record Inspecao5SResponse(
        Long idInspecao,
        Long idSetor,
        String setorNome,
        Long idInspetor,
        LocalDate dataInspecao,
        TurnoInspecao turno,
        StatusInspecao status,
        String observacoes,
        List<ItemInspecaoResponse> itens) {

    public static Inspecao5SResponse of(Inspecao5S inspecao, List<ItemInspecaoResponse> itens) {
        return new Inspecao5SResponse(
                inspecao.getIdInspecao(),
                inspecao.getSetor().getIdSetor(),
                inspecao.getSetor().getNome(),
                inspecao.getIdInspetor(),
                inspecao.getDataInspecao(),
                inspecao.getTurno(),
                inspecao.getStatus(),
                inspecao.getObservacoes(),
                itens);
    }
}