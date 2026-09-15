package com.fablab.rh.dto;

import com.fablab.rh.entity.NivelAcesso;

/**
 * Resposta com os dados de um funcionário.
 */
public record FuncionarioResponse(
        Long id,
        Long idPessoa,
        String nomePessoa,
        NivelAcesso nivelAcesso,
        String departamento) {
}