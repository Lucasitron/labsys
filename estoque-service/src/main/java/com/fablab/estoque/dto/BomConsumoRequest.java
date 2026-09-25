package com.fablab.estoque.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Requisição de registro de consumo real de itens da BOM.
 */
public record BomConsumoRequest(
        @Valid @NotEmpty(message = "Informe ao menos um item consumido") List<BomConsumoItem> itens) {
}