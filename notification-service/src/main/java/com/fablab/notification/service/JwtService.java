package com.fablab.notification.service;

import com.fablab.notification.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Validação e extração de claims dos tokens JWT emitidos pelo Auth &amp;
 * Identity Service (chave HMAC compartilhada).
 */
@Service
public class JwtService {

    public static final String CLAIM_ID_USER = "id_user";

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        if (properties.secret() == null || properties.secret().length() < 32) {
            throw new IllegalStateException("JWT_SECRET deve ter no mínimo 32 caracteres");
        }
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.issuer())
                .clockSkewSeconds(5)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}