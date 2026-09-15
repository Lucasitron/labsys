package com.fablab.vendas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Evento {@code orcamento.aprovado.event} publicado para o Financeiro Service.
 */
public record OrcamentoAprovadoEvent(
        Long idOrcamento,
        Long idCliente,
        BigDecimal valorTotal,
        LocalDate dataAprovacao) {
}