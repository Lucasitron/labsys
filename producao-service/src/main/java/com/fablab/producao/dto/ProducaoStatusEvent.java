package com.fablab.producao.dto;

/**
 * Evento {@code producao.status.alterado.event} publicado pelo Produção e
 * consumido pelo Vendas para sincronizar o Kanban.
 */
public record ProducaoStatusEvent(
        Long idEncomenda,
        String statusNovo,
        Long idUsuario,
        String observacao) {
}