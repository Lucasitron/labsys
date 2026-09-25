package com.fablab.rh.dto;

import com.fablab.rh.entity.NivelAcesso;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Convite de novo membro (Admin): cria pessoa + vínculo de funcionário.
 *
 * @param nomeCompleto nome da pessoa
 * @param matricula    matrícula única
 * @param contato      telefone ou e-mail
 * @param departamento departamento ou área de atuação
 * @param nivelAcesso  nível inicial (padrão: BOLSISTA)
 */
public record ConviteRequest(
        @NotBlank(message = "nomeCompleto é obrigatório")
        @Size(max = 255, message = "nomeCompleto deve ter no máximo 255 caracteres")
        String nomeCompleto,

        @NotBlank(message = "matricula é obrigatório")
        @Size(max = 64, message = "matricula deve ter no máximo 64 caracteres")
        String matricula,

        @Size(max = 255, message = "contato deve ter no máximo 255 caracteres")
        String contato,

        @Size(max = 255, message = "departamento deve ter no máximo 255 caracteres")
        String departamento,

        NivelAcesso nivelAcesso) {
}
