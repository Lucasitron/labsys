package com.fablab.estoque.dto;

/**
 * Resposta de localização física (armário, prateleira, caixa).
 */
public record LocalizacaoResponse(
        Long id,
        String armario,
        String prateleira,
        String caixa,
        String descricao) {
}