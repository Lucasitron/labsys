package com.fablab.vendas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades de validação do token JWT emitido pelo Auth &amp; Identity Service.
 *
 * @param secret chave HMAC compartilhada de assinatura (mín. 32 caracteres)
 * @param issuer emissor esperado dos tokens
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, String issuer) {
}