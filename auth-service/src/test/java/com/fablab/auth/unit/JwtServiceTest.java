package com.fablab.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fablab.auth.TestData;
import com.fablab.auth.config.JwtProperties;
import com.fablab.auth.dto.LoginPrincipal;
import com.fablab.auth.entity.Login;
import com.fablab.auth.entity.Role;
import com.fablab.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import io.jsonwebtoken.JwtException;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(TestData.SECRET, 900, 604800, "fablab-test"));
    }

    private Login login() {
        return TestData.login(1L, 7L, "CARD-001", "admin@fablab.io", "admin", "Direção");
    }

    @Test
    void generatesAccessTokenWithExpectedClaims() {
        String token = jwtService.generateAccessToken(login(), Role.ADMIN);

        Claims claims = jwtService.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(jwtService.extractIdUser(claims)).isEqualTo(7L);
        assertThat(jwtService.extractRole(claims)).isEqualTo(Role.ADMIN);
        assertThat(jwtService.extractSetor(claims)).isEqualTo("Direção");
        assertThat(jwtService.extractType(claims)).isEqualTo(JwtService.TYPE_ACCESS);
        assertThat(claims.getIssuer()).isEqualTo("fablab-test");
    }

    @Test
    void generatesRefreshTokenWithLongerExpiration() {
        String access = jwtService.generateAccessToken(login(), Role.ADMIN);
        String refresh = jwtService.generateRefreshToken(login(), Role.ADMIN);

        Claims accessClaims = jwtService.parseClaims(access);
        Claims refreshClaims = jwtService.parseClaims(refresh);

        assertThat(jwtService.extractType(refreshClaims)).isEqualTo(JwtService.TYPE_REFRESH);
        Duration accessLifetime = Duration.between(accessClaims.getIssuedAt().toInstant(),
                accessClaims.getExpiration().toInstant());
        Duration refreshLifetime = Duration.between(refreshClaims.getIssuedAt().toInstant(),
                refreshClaims.getExpiration().toInstant());
        assertThat(accessLifetime.toSeconds()).isEqualTo(900);
        assertThat(refreshLifetime.toSeconds()).isEqualTo(604800);
        assertThat(refreshLifetime).isGreaterThan(accessLifetime);
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.generateAccessToken(login(), Role.ADMIN);
        String tampered = token.substring(0, token.length() - 3) + "abc";

        assertThat(jwtService.isTokenValid(tampered)).isFalse();
        assertThatThrownBy(() -> jwtService.parseClaims(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() {
        JwtService shortLived = new JwtService(
                new JwtProperties(TestData.SECRET, -5, 604800, "fablab-test"));
        String token = shortLived.generateAccessToken(login(), Role.ADMIN);

        assertThat(shortLived.isTokenValid(token)).isFalse();
        assertThatThrownBy(() -> shortLived.parseClaims(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokenSignedByAnotherIssuer() {
        JwtService issuerA = new JwtService(
                new JwtProperties(TestData.SECRET, 900, 604800, "issuer-a"));
        JwtService issuerB = new JwtService(
                new JwtProperties(TestData.SECRET, 900, 604800, "issuer-b"));

        String token = issuerA.generateAccessToken(login(), Role.ADMIN);

        assertThat(issuerB.isTokenValid(token)).isFalse();
        assertThatThrownBy(() -> issuerB.parseClaims(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsRefreshTokenAsAccessTokenInIsRefreshCheck() {
        String refresh = jwtService.generateRefreshToken(login(), Role.ADMIN);
        assertThat(jwtService.isRefreshToken(jwtService.parseClaims(refresh))).isTrue();
        String access = jwtService.generateAccessToken(login(), Role.ADMIN);
        assertThat(jwtService.isRefreshToken(jwtService.parseClaims(access))).isFalse();
    }

    @Test
    void rejectsSecretShorterThan32Bytes() {
        assertThatThrownBy(() -> new JwtService(
                new JwtProperties("short", 900, 604800, "fablab-test")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void accessExpirationSecondsExposesConfiguredValue() {
        assertThat(jwtService.accessExpirationSeconds()).isEqualTo(900);
    }

    @Test
    void loginPrincipalDerivesRoleAuthority() {
        LoginPrincipal principal = new LoginPrincipal(1L, 7L, Role.BOLSISTA, "Laboratório");
        assertThat(principal.authorities())
                .extracting("authority")
                .containsExactly("ROLE_BOLSISTA");
    }

    @Test
    void claimsExpirationIsInFutureForValidToken() {
        String token = jwtService.generateAccessToken(login(), Role.ADMIN);
        Claims claims = jwtService.parseClaims(token);
        assertThat(claims.getExpiration()).isAfter(new Date());
    }
}