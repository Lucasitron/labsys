package com.fablab.rh.dto;

/**
 * Resposta com os dados de um treinamento.
 */
public record TreinamentoResponse(
        Long id,
        String titulo,
        String descricao,
        String urlConteudo,
        Long idTutor) {
}