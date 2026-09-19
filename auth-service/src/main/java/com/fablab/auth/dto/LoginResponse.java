package com.fablab.auth.dto;

/**
 * Resposta do {@code POST /auth/login}.
 *
 * @param token     JWT a ser enviado como {@code Authorization: Bearer <token>}
 * @param expiresIn validade do token em segundos
 * @param user      perfil do usuário autenticado
 */
public record LoginResponse(String token, int expiresIn, UserResponse user) {
}