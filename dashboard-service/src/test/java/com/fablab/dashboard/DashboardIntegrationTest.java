package com.fablab.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Fluxo HTTP do resumo do dashboard e da conclusão de tarefas.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DashboardIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private HttpHeaders bearerAdmin() {
        long agora = System.currentTimeMillis();
        String token = Jwts.builder()
                .subject("admin")
                .claims(Map.of("id_user", 1L, "role", "ADMIN", "setor", ""))
                .issuer("fablab-test")
                .issuedAt(new java.util.Date(agora))
                .expiration(new java.util.Date(agora + 3600_000))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void summaryExigeAutenticacao() {
        ResponseEntity<String> response = restTemplate.getForEntity("/dashboard/summary", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void summaryRetornaEstruturaEsperada() {
        ResponseEntity<String> response = restTemplate.exchange("/dashboard/summary", HttpMethod.GET,
                new HttpEntity<>(null, bearerAdmin()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("\"tasks\"", "\"kpis\"", "\"ordersActive\"", "\"notificationsUnread\"");
    }

    @Test
    void patchTaskConcluiTarefa() {
        ResponseEntity<String> response = restTemplate.exchange("/tasks/rh-cert-42", HttpMethod.PATCH,
                new HttpEntity<>("{\"done\":true}", headersJson(bearerAdmin())), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"done\":true", "\"rh-cert-42\"");
    }

    @Test
    void patchTaskRejeitaUndo() {
        ResponseEntity<String> response = restTemplate.exchange("/tasks/rh-cert-42", HttpMethod.PATCH,
                new HttpEntity<>("{\"done\":false}", headersJson(bearerAdmin())), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private HttpHeaders headersJson(HttpHeaders headers) {
        headers.set("Content-Type", "application/json");
        return headers;
    }
}