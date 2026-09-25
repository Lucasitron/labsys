package com.fablab.estoque.dto;

import com.fablab.estoque.entity.Categoria;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Requisição de criação/atualização de item.
 */
public record ItemRequest(
        @NotBlank(message = "O nome do item é obrigatório") String nome,
        String descricao,
        @NotNull(message = "A categoria é obrigatória") Categoria categoria,
        @NotBlank(message = "A unidade de medida é obrigatória") String unidadeMedida,
        @NotNull(message = "A quantidade atual é obrigatória")
        @DecimalMin(value = "0", message = "A quantidade atual não pode ser negativa") BigDecimal quantidadeAtual,
        @NotNull(message = "O estoque mínimo é obrigatório")
        @DecimalMin(value = "0", message = "O estoque mínimo não pode ser negativo") BigDecimal estoqueMinimo,
        Long idLocalizacao) {
}