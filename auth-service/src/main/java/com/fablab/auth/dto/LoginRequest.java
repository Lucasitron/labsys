package com.fablab.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Requisição de autenticação por e-mail ou nome de usuário e senha.
 *
 * <p>Pelo menos um dos identificadores ({@code email} ou {@code nomeUsuario})
 * deve ser informado.</p>
 */
public record LoginRequest(

        @Email(message = "e-mail inválido")
        String email,

        String nomeUsuario,

        @NotBlank(message = "senha é obrigatória")
        @Size(min = 6, max = 72, message = "senha deve ter entre 6 e 72 caracteres")
        String senha) {
}