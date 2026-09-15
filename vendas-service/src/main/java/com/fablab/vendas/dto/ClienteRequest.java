package com.fablab.vendas.dto;

import com.fablab.vendas.entity.TipoPessoa;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Payload de criação/atualização de cliente (PF ou PJ).
 *
 * @param cpfCnpj documento validado conforme {@code tipoPessoa}
 */
public record ClienteRequest(
        @NotNull TipoPessoa tipoPessoa,
        @NotBlank @Size(max = 200) String nomeRazaoSocial,
        @NotBlank @Size(max = 20) String cpfCnpj,
        @Email @Size(max = 150) String email,
        @Size(max = 30) String telefone,
        @Size(max = 300) String endereco,
        List<@NotNull Long> tags) {

    @Pattern(regexp = "\\d+", message = "deve conter apenas dígitos")
    public String cpfCnpj() {
        return cpfCnpj;
    }
}