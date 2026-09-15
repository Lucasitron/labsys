package com.fablab.vendas.dto;

import com.fablab.vendas.entity.Encomenda;
import com.fablab.vendas.entity.StatusKanban;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resposta de encomenda com os dados do Kanban.
 */
public record EncomendaResponse(
        Long id,
        Long idCliente,
        String clienteNome,
        Long idOrcamento,
        LocalDate dataCriacao,
        LocalDate dataPrevisaoEntrega,
        StatusKanban statusKanban,
        BigDecimal valorFinal,
        String observacoes) {

    public static EncomendaResponse of(Encomenda encomenda) {
        return new EncomendaResponse(
                encomenda.getId(),
                encomenda.getCliente().getId(),
                encomenda.getCliente().getNomeRazaoSocial(),
                encomenda.getOrcamento() == null ? null : encomenda.getOrcamento().getId(),
                encomenda.getDataCriacao(),
                encomenda.getDataPrevisaoEntrega(),
                encomenda.getStatusKanban(),
                encomenda.getValorFinal(),
                encomenda.getObservacoes());
    }
}