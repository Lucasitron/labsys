package com.fablab.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Requisição de alteração de senha pelo próprio usuário autenticado
 * ({@code PUT /auth/senha}).
 */
public record AlterarSenhaRequest(

        @NotBlank(message = "senha atual é obrigatória")
        String senhaAtual,

        @NotBlank(message = "nova senha é obrigatória")
        @Size(min = 8, max = 72, message = "nova senha deve ter ao menos 8 caracteres")
        String novaSenha,

        @NotBlank(message = "confirmação de senha é obrigatória")
        String confirmacaoSenha) {
}
