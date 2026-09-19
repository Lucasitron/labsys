package com.fablab.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades de emissão/validação do token JWT.
 *
 * @param secret           chave HMAC compartilhada de assinatura (mín. 32 caracteres)
 * @param expirationSeconds validade do token em segundos
 * @param issuer           emissor dos tokens
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, int expirationSeconds, String issuer) {
}