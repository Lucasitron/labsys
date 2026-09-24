package com.fablab.rh.dto;

import com.fablab.rh.entity.PessoaStatus;
import java.time.LocalDate;

/**
 * Resposta com os dados de uma pessoa (CPF sempre mascarado — LGPD).
 */
public record PessoaResponse(
        Long id,
        String nomeCompleto,
        String matricula,
        LocalDate dataAdmissao,
        String contato,
        String turno,
        PessoaStatus status,
        String cpf) {
}