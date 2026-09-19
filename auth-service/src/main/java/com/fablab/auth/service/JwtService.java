package com.fablab.auth.service;

import com.fablab.auth.config.JwtProperties;
import com.fablab.auth.dto.AuthPrincipal;
import com.fablab.auth.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Emissão e validação de tokens JWT. A chave HMAC e o emissor são
 * compartilhados com os demais serviços (variáveis {@code JWT_SECRET} e
 * {@code JWT_ISSUER}), mantendo a malha interoperável.
 */
@Service
public class JwtService {

    public static final String CLAIM_ID_USER = "id_user";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_SETOR = "setor";

    private static final String[] ROLES = {
        "ADMIN", "BOLSISTA", "VOLUNTARIO", "ESTAGIARIO", "RECRUTANDO"
    };

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
     * Gera um token para o usuário com os claims {@code id_user}, {@code role}
     * (nome do nível) e {@code setor}, válido por {@code jwt.expiration-seconds}.
     */
    public String emitir(Usuario usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(usuario.getUsername())
                .claim(CLAIM_ID_USER, usuario.getId())
                .claim(CLAIM_ROLE, roleName(usuario.getRole()))
                .claim(CLAIM_SETOR, usuario.getSetor())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(properties.expirationSeconds(), ChronoUnit.SECONDS)))
                .signWith(key)
                .compact();
    }

    /**
     * Valida assinatura, emissor e expiração e retorna as claims do token.
     *
     * @throws io.jsonwebtoken.JwtException se o token for inválido
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

    public Long extractIdUser(Claims claims) {
        return ((Number) claims.get(CLAIM_ID_USER)).longValue();
    }

    public String extractRole(Claims claims) {
        return claims.get(CLAIM_ROLE, String.class);
    }

    public String extractSetor(Claims claims) {
        return claims.get(CLAIM_SETOR, String.class);
    }

    public AuthPrincipal principal(Claims claims) {
        return new AuthPrincipal(
                extractIdUser(claims),
                claims.getSubject(),
                extractRole(claims),
                extractSetor(claims));
    }

    /** Nome do nível de acesso usado no claim {@code role} (compatível com o RH). */
    public static String roleName(Integer role) {
        if (role == null || role < 0 || role >= ROLES.length) {
            throw new IllegalArgumentException("Nível de acesso inválido: " + role);
        }
        return ROLES[role];
    }
}