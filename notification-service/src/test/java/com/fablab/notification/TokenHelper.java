package com.fablab.notification;

import com.fablab.notification.entity.NivelAcesso;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;

/** Gera tokens JWT de teste compatíveis com o {@code JwtService} do serviço. */
public final class TokenHelper {

    public static final String SECRET = "fablab-notification-test-secret-key-min-32-bytes-123456";
    public static final String ISSUER = "fablab-test";

    private TokenHelper() {
    }

    private static SecretKey key() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    public static String token(Long idPessoa, NivelAcesso nivel) {
        return tokenWithRole(idPessoa, nivel.name());
    }

    public static String tokenWithRole(Long idPessoa, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject("1")
                .claim("id_user", idPessoa)
                .claim("role", role)
                .claim("setor", "FabLab")
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(900)))
                .signWith(key())
                .compact();
    }
}
