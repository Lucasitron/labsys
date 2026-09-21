package com.fablab.estoque.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Requisição de criação/atualização de Lista de Materiais (BOM).
 */
public record BomRequest(
        @NotNull(message = "O produto/serviço é obrigatório") Long idProdutoServico,
        @NotBlank(message = "O nome da BOM é obrigatório") String nome,
        Integer versao,
        Boolean editavel,
        @Valid @NotEmpty(message = "A BOM deve conter ao menos um item") List<BomItemRequest> itens) {
}