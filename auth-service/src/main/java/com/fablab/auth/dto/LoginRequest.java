package com.fablab.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Requisição de autenticação por e-mail ou nome de usuário e senha.
 *
 * <p>Pelo menos um dos identificadores ({@code email} ou {@code nomeUsuario})
 * deve ser informado. Aliases {@code username}/{@code password} aceitos para
 * compatibilidade com o payload da UI ({@code POST /auth/login
 * {username, password}}), sem churn no front concluído.</p>
 */
public record LoginRequest(

        @Email(message = "e-mail inválido")
        String email,

        @JsonAlias("username")
        String nomeUsuario,

        @JsonAlias("password")
        @NotBlank(message = "senha é obrigatória")
        @Size(min = 6, max = 72, message = "senha deve ter entre 6 e 72 caracteres")
        String senha) {
}