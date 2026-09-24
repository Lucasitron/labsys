package com.fablab.auth.dto;

/**
 * Resposta do login com o par de tokens JWT (acesso + refresh).
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        Long idUser,
        String role,
        String setor,
        String nomeUsuario) {
}