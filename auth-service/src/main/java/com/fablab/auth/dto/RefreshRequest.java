package com.fablab.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Requisição de renovação de token usando o refresh token.
 */
public record RefreshRequest(
        @NotBlank(message = "refreshToken é obrigatório")
        String refreshToken) {
}