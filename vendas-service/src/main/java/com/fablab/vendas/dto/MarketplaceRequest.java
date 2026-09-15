package com.fablab.vendas.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload de registro manual de venda em marketplace.
 */
public record MarketplaceRequest(
        @NotNull Long idEncomenda,
        @NotNull @Size(max = 100) String plataforma,
        @Size(max = 100) String codigoExterno,
        LocalDate dataVenda,
        @PositiveOrZero BigDecimal valorTaxa) {
}