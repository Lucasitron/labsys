package com.fablab.vendas;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Gera JWTs válidos para os testes (mesmo esquema do Auth Service). */
@Component
public class TokenHelper {

    private final SecretKey key;
    private final String issuer;

    public TokenHelper(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer}") String issuer) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
    }

    public String token(Long idUser, String role) {
        return token(idUser, role, "vendas");
    }

    public String token(Long idUser, String role, String setor) {
        long agora = System.currentTimeMillis();
        return Jwts.builder()
                .issuer(issuer)
                .subject("teste")
                .claim("id_user", idUser)
                .claim("role", role)
                .claim("setor", setor)
                .issuedAt(new Date(agora))
                .expiration(new Date(agora + 3600_000))
                .signWith(key)
                .compact();
    }
}
