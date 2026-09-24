package com.fablab.rh.dto;

import com.fablab.rh.entity.PessoaStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Dados de cadastro/atualização de uma pessoa.
 *
 * @param nomeCompleto nome da pessoa
 * @param matricula    matrícula única
 * @param dataAdmissao data de entrada no laboratório
 * @param contato      telefone ou e-mail
 * @param turno        turno de trabalho/estudo
 * @param status       status (padrão: ATIVO)
 * @param cpf          CPF opcional (11 dígitos ou formato 000.000.000-00;
 *                     respondido sempre mascarado por LGPD)
 */
public record PessoaRequest(
        @NotBlank(message = "nomeCompleto é obrigatório")
        @Size(max = 255, message = "nomeCompleto deve ter no máximo 255 caracteres")
        String nomeCompleto,

        @NotBlank(message = "matricula é obrigatório")
        @Size(max = 64, message = "matricula deve ter no máximo 64 caracteres")
        String matricula,

        LocalDate dataAdmissao,

        @Size(max = 255, message = "contato deve ter no máximo 255 caracteres")
        String contato,

        @Size(max = 32, message = "turno deve ter no máximo 32 caracteres")
        String turno,

        PessoaStatus status,

        @Pattern(regexp = "^$|\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}",
                message = "cpf deve ter 11 dígitos ou o formato 000.000.000-00")
        String cpf) {
}