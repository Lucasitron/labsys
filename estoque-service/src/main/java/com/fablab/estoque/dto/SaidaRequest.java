package com.fablab.estoque.dto;

import com.fablab.estoque.entity.TipoSaida;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Requisição de saída manual de estoque (consumo, perda, ajuste).
 */
public record SaidaRequest(
        @NotNull(message = "O item é obrigatório") Long idItem,
        @NotNull(message = "A quantidade é obrigatória")
        @DecimalMin(value = "0.01", message = "A quantidade deve ser maior que zero") BigDecimal quantidade,
        @NotNull(message = "O tipo de saída é obrigatório") TipoSaida tipoSaida,
        Long idReferencia,
        String observacao,
        @Size(max = 255, message = "O responsável deve ter no máximo 255 caracteres") String responsavel) {
}