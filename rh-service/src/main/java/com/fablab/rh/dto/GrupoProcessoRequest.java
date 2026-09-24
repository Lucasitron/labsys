package com.fablab.rh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Criação de um grupo do processo seletivo.
 *
 * @param nome      nome do grupo (ex.: {@code Eletrônica})
 * @param idLider   id do funcionário tutor líder do grupo
 * @param membroIds ids das pessoas candidatas (com processo seletivo aberto)
 */
public record GrupoProcessoRequest(
        @NotBlank(message = "nome é obrigatório")
        @Size(max = 255, message = "nome deve ter no máximo 255 caracteres")
        String nome,

        @NotNull(message = "idLider é obrigatório")
        Long idLider,

        List<Long> membroIds) {
}
