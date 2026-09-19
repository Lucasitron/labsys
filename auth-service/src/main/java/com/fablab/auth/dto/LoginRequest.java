package com.fablab.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Credenciais enviadas no {@code POST /auth/login}.
 *
 * @param username usuário de acesso
 * @param password senha em texto puro (comparada ao hash via BCrypt)
 */
public record LoginRequest(
        @NotBlank(message = "Informe o usuário") String username,
        @NotBlank(message = "Informe a senha") String password) {
}