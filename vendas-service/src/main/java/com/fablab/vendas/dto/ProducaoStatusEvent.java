package com.fablab.vendas.dto;

import java.time.LocalDateTime;

/**
 * Evento {@code producao.status.alterado.event} consumido do Produção &amp;
 * Projetos Service para atualizar o Kanban de uma encomenda.
 */
public record ProducaoStatusEvent(
        Long idEncomenda,
        String statusNovo,
        Long idUsuario,
        String observacao) {
}