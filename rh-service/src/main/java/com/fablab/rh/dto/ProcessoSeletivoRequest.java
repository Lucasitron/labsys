package com.fablab.rh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados para iniciar um processo seletivo (cria pessoa e funcionário com
 * nível "Recrutando").
 *
 * @param nomeCompleto nome do candidato
 * @param matricula    matrícula do candidato
 * @param contato      telefone ou e-mail
 * @param turno        turno pretendido
 * @param idTutor      id do funcionário tutor responsável
 */
public record ProcessoSeletivoRequest(
        @NotBlank(message = "nomeCompleto é obrigatório")
        @Size(max = 255, message = "nomeCompleto deve ter no máximo 255 caracteres")
        String nomeCompleto,

        @NotBlank(message = "matricula é obrigatório")
        @Size(max = 64, message = "matricula deve ter no máximo 64 caracteres")
        String matricula,

        @Size(max = 255, message = "contato deve ter no máximo 255 caracteres")
        String contato,

        @Size(max = 32, message = "turno deve ter no máximo 32 caracteres")
        String turno,

        @NotNull(message = "idTutor é obrigatório")
        Long idTutor) {
}