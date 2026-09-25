package com.fablab.estoque.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Item de uma Lista de Materiais (BOM) na requisição de criação/atualização.
 */
public record BomItemRequest(
        @NotNull(message = "O item da BOM é obrigatório") Long idItem,
        @NotNull(message = "A quantidade prevista é obrigatória")
        @DecimalMin(value = "0.01", message = "A quantidade prevista deve ser maior que zero") BigDecimal quantidadePrevista) {
}