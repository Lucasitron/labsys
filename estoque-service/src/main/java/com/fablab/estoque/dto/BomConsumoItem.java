package com.fablab.estoque.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Item consumido durante a produção (consumo real da BOM).
 */
public record BomConsumoItem(
        @NotNull(message = "O item consumido é obrigatório") Long idItem,
        @NotNull(message = "A quantidade consumida é obrigatória")
        @DecimalMin(value = "0.01", message = "A quantidade consumida deve ser maior que zero") BigDecimal quantidadeConsumida) {
}