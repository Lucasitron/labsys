package com.fablab.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.fablab.notification.entity.Notificacao;
import com.fablab.notification.repository.NotificacaoRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
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
 * Contagem de não lidas pelo HTTP real.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificacaoIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @BeforeEach
    void setUp() {
        notificacaoRepository.deleteAll();
    }

    private HttpHeaders bearer(long idUsuario) {
        long agora = System.currentTimeMillis();
        String token = Jwts.builder()
                .subject("usuario-" + idUsuario)
                .claims(Map.of("id_user", idUsuario, "role", "ADMIN", "setor", ""))
                .issuer("fablab-test")
                .issuedAt(new java.util.Date(agora))
                .expiration(new java.util.Date(agora + 3600_000))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private Notificacao notificacao(long idUsuario, boolean lida) {
        Notificacao notificacao = new Notificacao();
        notificacao.setIdUsuario(idUsuario);
        notificacao.setTitulo("Certificado disponível");
        notificacao.setMensagem("Seu certificado foi emitido.");
        notificacao.setLida(lida);
        notificacao.setCriadaEm(Instant.now());
        return notificacao;
    }

    @Test
    void unreadCountExigeAutenticacao() {
        ResponseEntity<String> response = restTemplate.getForEntity("/notifications/unread/count", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void unreadCountContaSomenteNaoLidasDoUsuario() {
        notificacaoRepository.save(notificacao(7L, false));
        notificacaoRepository.save(notificacao(7L, false));
        notificacaoRepository.save(notificacao(7L, true));
        notificacaoRepository.save(notificacao(8L, false));

        ResponseEntity<String> response = restTemplate.exchange("/notifications/unread/count", HttpMethod.GET,
                new HttpEntity<>(null, bearer(7L)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"count\":2");
    }

    @Test
    void unreadCountZeroSemNotificacoes() {
        ResponseEntity<String> response = restTemplate.exchange("/notifications/unread/count", HttpMethod.GET,
                new HttpEntity<>(null, bearer(9L)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"count\":0");
    }
}