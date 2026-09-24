package com.fablab.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades de configuração do token JWT.
 *
 * @param secret                  chave HMAC de assinatura (mín. 32 caracteres)
 * @param expirationSeconds       validade do token de acesso (padrão: 900s)
 * @param refreshExpirationSeconds validade do refresh token (padrão: 604800s)
 * @param issuer                  emissor do token
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long expirationSeconds,
        long refreshExpirationSeconds,
        String issuer) {
}