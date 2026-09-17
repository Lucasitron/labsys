package com.fablab.financeiro.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Evento {@code encomenda.criada.event} consumido do Vendas &amp; CRM Service.
 * Dispara a criação do fechamento da encomenda em {@code ABERTA}.
 *
 * @param idEncomenda id da encomenda no Vendas
 * @param idCliente id do cliente
 * @param status status inicial (Kanban {@code FILA})
 * @param valorFinal valor final da encomenda
 * @param dataCriacao data de criação
 */
public record EncomendaCriadaEvent(
        Long idEncomenda,
        Long idCliente,
        String status,
        BigDecimal valorFinal,
        LocalDate dataCriacao) {
}