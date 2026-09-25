package com.fablab.estoque.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Requisição de cadastro de localização física (tela 19 do front).
 */
public record LocalizacaoRequest(
        @NotBlank(message = "O armário é obrigatório")
        @Size(max = 255, message = "O armário deve ter no máximo 255 caracteres") String armario,
        String prateleira,
        String caixa,
        String descricao) {
}