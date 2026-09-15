package com.fablab.vendas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Evento {@code encomenda.criada.event} publicado para Produção &amp; Projetos
 * e Notification Service.
 */
public record EncomendaCriadaEvent(
        Long idEncomenda,
        Long idCliente,
        String statusKanban,
        BigDecimal valorFinal,
        LocalDate dataCriacao) {
}