package com.fablab.rh.dto;

import com.fablab.rh.entity.NivelAcesso;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados de criação de um funcionário vinculado a uma pessoa.
 *
 * @param idPessoa     pessoa vinculada
 * @param nivelAcesso  nível inicial (padrão: BOLSISTA)
 * @param departamento departamento ou área de atuação
 */
public record FuncionarioRequest(
        @NotNull(message = "idPessoa é obrigatório")
        Long idPessoa,

        NivelAcesso nivelAcesso,

        @Size(max = 255, message = "departamento deve ter no máximo 255 caracteres")
        String departamento) {

    public NivelAcesso nivelOrDefault() {
        return nivelAcesso == null ? NivelAcesso.BOLSISTA : nivelAcesso;
    }
}