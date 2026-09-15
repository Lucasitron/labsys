package com.fablab.vendas.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload de criação de encomenda a partir de um orçamento aprovado.
 *
 * @param idOrcamento orçamento aprovado de origem (pode ser nulo em vendas
 *                    diretas, como marketplace)
 */
public record EncomendaRequest(
        Long idOrcamento,
        @NotNull Long idCliente,
        LocalDate dataPrevisaoEntrega,
        @Positive BigDecimal valorFinal,
        @Size(max = 500) String observacoes) {
}