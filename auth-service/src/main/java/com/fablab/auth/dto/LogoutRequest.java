package com.fablab.auth.dto;

/**
 * Requisição de logout. O token pode ser informado no corpo; quando ausente,
 * o serviço utiliza o token do cabeçalho {@code Authorization}.
 */
public record LogoutRequest(String token) {
}