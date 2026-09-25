package com.fablab.rh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Criação de um treinamento (LMS) por um tutor.
 *
 * @param titulo      título do treinamento
 * @param descricao   descrição do conteúdo
 * @param urlConteudo link para guia/documentação
 * @param idTutor     id do funcionário tutor criador (se nulo, utiliza o
 *                    funcionário do token)
 */
public record TreinamentoRequest(
        @NotBlank(message = "titulo é obrigatório")
        @Size(max = 255, message = "titulo deve ter no máximo 255 caracteres")
        String titulo,

        String descricao,

        @Size(max = 500, message = "urlConteudo deve ter no máximo 500 caracteres")
        String urlConteudo,

        Long idTutor) {
}