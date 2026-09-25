package com.fablab.rh.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fablab.rh.TokenHelper;
import com.fablab.rh.config.JwtProperties;
import com.fablab.rh.service.JwtService;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(TokenHelper.SECRET, TokenHelper.ISSUER));
    }

    @Test
    void parseClaimsValidaTokenValido() {
        var claims = jwtService.parseClaims(TokenHelper.token(10L, com.fablab.rh.entity.NivelAcesso.ADMIN));

        assertThat(jwtService.extractIdUser(claims)).isEqualTo(10L);
        assertThat(jwtService.extractRole(claims)).isEqualTo("ADMIN");
    }

    @Test
    void parseClaimsRejeitaSecretDiferente() {
        String token = Jwts.builder()
                .issuer(TokenHelper.ISSUER)
                .subject("1")
                .claim("id_user", 10L)
                .claim("role", "ADMIN")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(900)))
                .signWith(Keys.hmacShaKeyFor(
                        "outra-chave-test-min-32-bytes-1234567890-ok".getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwtService.parseClaims(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parseClaimsRejeitaIssuerDiferente() {
        String token = Jwts.builder()
                .issuer("outro-emissor")
                .subject("1")
                .claim("id_user", 10L)
                .claim("role", "ADMIN")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(900)))
                .signWith(Keys.hmacShaKeyFor(TokenHelper.SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwtService.parseClaims(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parseClaimsRejeitaTokenExpirado() {
        String token = Jwts.builder()
                .issuer(TokenHelper.ISSUER)
                .subject("1")
                .claim("id_user", 10L)
                .claim("role", "ADMIN")
                .issuedAt(Date.from(Instant.now().minusSeconds(3600)))
                .expiration(Date.from(Instant.now().minusSeconds(1800)))
                .signWith(Keys.hmacShaKeyFor(TokenHelper.SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwtService.parseClaims(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void construtorRejeitaSecretCurto() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("curto", TokenHelper.ISSUER)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 caracteres");
    }
}