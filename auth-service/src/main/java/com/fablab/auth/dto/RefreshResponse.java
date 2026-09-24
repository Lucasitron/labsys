package com.fablab.auth.dto;

/**
 * Resposta da renovação de token. O refresh token antigo é rotacionado
 * (adicionado à blacklist).
 */
public record RefreshResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn) {
}