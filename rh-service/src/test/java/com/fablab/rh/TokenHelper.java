package com.fablab.rh;

import com.fablab.rh.entity.NivelAcesso;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;

/**
 * Gera tokens JWT de teste compatíveis com o {@code JwtService} do serviço.
 */
public final class TokenHelper {

    public static final String SECRET = "fablab-rh-test-secret-key-min-32-bytes-1234567890";
    public static final String ISSUER = "fablab-test";

    private TokenHelper() {
    }

    private static SecretKey key() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    public static String token(Long idPessoa, NivelAcesso nivel) {
        return token(idPessoa, nivel, null);
    }

    public static String token(Long idPessoa, NivelAcesso nivel, Long idFuncionario) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject("1")
                .claim("id_user", idPessoa)
                .claim("role", nivel.name())
                .claim("setor", "FabLab")
                .claim("type", "access")
                .claim("id_funcionario", idFuncionario)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(900)))
                .signWith(key())
                .compact();
    }
}