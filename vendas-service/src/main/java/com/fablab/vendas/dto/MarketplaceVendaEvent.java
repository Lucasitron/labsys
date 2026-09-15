package com.fablab.vendas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Evento {@code marketplace.venda.event} publicado para o Financeiro Service.
 */
public record MarketplaceVendaEvent(
        Long idRegistro,
        Long idEncomenda,
        String plataforma,
        LocalDate dataVenda,
        BigDecimal valorTaxa) {
}