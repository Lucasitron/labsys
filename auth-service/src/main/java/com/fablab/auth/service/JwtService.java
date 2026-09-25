package com.fablab.auth.service;

import com.fablab.auth.config.JwtProperties;
import com.fablab.auth.entity.Login;
import com.fablab.auth.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Geração, assinatura e extração de claims de tokens JWT (HMAC SHA-256).
 *
 * <p>O payload contém {@code id_user}, {@code role} e {@code setor}. Os tokens de
 * acesso duram 15 minutos; os refresh tokens, 7 dias.</p>
 */
@Service
public class JwtService {

    public static final String CLAIM_ID_USER = "id_user";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_SETOR = "setor";
    public static final String CLAIM_TYPE = "type";

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        if (properties.secret() == null || properties.secret().length() < 32) {
            throw new IllegalStateException("JWT_SECRET deve ter no mínimo 32 caracteres");
        }
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Gera um token de acesso (15 minutos).
     */
    public String generateAccessToken(Login login, Role role) {
        return generate(login, role, TYPE_ACCESS, properties.expirationSeconds());
    }

    /**
     * Gera um refresh token (7 dias).
     */
    public String generateRefreshToken(Login login, Role role) {
        return generate(login, role, TYPE_REFRESH, properties.refreshExpirationSeconds());
    }

    private String generate(Login login, Role role, String type, long seconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(String.valueOf(login.getId()))
                .claim(CLAIM_ID_USER, login.getIdUser())
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_SETOR, login.getSetor())
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(seconds)))
                .signWith(key)
                .compact();
    }

    /**
     * Valida a assinatura, emissor e expiração e retorna as claims do token.
     *
     * @throws JwtException se o token for malformado, com assinatura inválida,
     *                      pertencer a outro emissor ou estiver expirado
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.issuer())
                .clockSkewSeconds(5)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Verifica se o token é assinável e não expirou.
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public Long extractIdUser(Claims claims) {
        return ((Number) claims.get(CLAIM_ID_USER)).longValue();
    }

    public Role extractRole(Claims claims) {
        return Role.valueOf(claims.get(CLAIM_ROLE, String.class));
    }

    public String extractSetor(Claims claims) {
        return claims.get(CLAIM_SETOR, String.class);
    }

    public String extractType(Claims claims) {
        return claims.get(CLAIM_TYPE, String.class);
    }

    public boolean isRefreshToken(Claims claims) {
        return TYPE_REFRESH.equals(extractType(claims));
    }

    public long accessExpirationSeconds() {
        return properties.expirationSeconds();
    }
}