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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * D-1..D-7 (§6.3) pelo HTTP real.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificacaoD1D7IntegrationTest {

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

    private HttpHeaders bearer(long idUsuario, String role) {
        long agora = System.currentTimeMillis();
        String token = Jwts.builder()
                .subject("usuario-" + idUsuario)
                .claims(Map.of("id_user", idUsuario, "role", role, "setor", ""))
                .issuer("fablab-test")
                .issuedAt(new java.util.Date(agora))
                .expiration(new java.util.Date(agora + 3600_000))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private Notificacao notificacao(long idUsuario, String tipo, String canal, boolean lida, String titulo) {
        Notificacao notificacao = new Notificacao();
        notificacao.setIdUsuario(idUsuario);
        notificacao.setTipo(tipo);
        notificacao.setCanal(canal);
        notificacao.setTitulo(titulo);
        notificacao.setMensagem("Detalhe de " + titulo);
        notificacao.setLida(lida);
        notificacao.setCriadaEm(Instant.now());
        return notificacao;
    }

    @Test
    void listaPaginadaIsoladaPorJwtComShapeCanonico() {
        notificacaoRepository.save(notificacao(7L, "estoque", "inapp", false, "Estoque baixo A"));
        notificacaoRepository.save(notificacao(7L, "estoque", "email", false, "Estoque baixo B"));
        notificacaoRepository.save(notificacao(7L, "pessoas", "inapp", true, "Aviso lido"));
        notificacaoRepository.save(notificacao(8L, "estoque", "inapp", false, "De outro usuário"));

        ResponseEntity<String> response = restTemplate.exchange("/notifications?page=1&size=2",
                HttpMethod.GET, new HttpEntity<>(null, bearer(7L, "BOLSISTA")), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"total\":3");
        assertThat(response.getBody()).contains("\"page\":1");
        assertThat(response.getBody()).contains("\"size\":2");
        assertThat(response.getBody()).contains("\"items\"");
        assertThat(response.getBody()).doesNotContain("De outro usuário");

        ResponseEntity<String> pagina2 = restTemplate.exchange("/notifications?page=2&size=2",
                HttpMethod.GET, new HttpEntity<>(null, bearer(7L, "BOLSISTA")), String.class);
        assertThat(pagina2.getBody()).contains("\"total\":3");
    }

    @Test
    void listaFiltraPorTipoERead() {
        notificacaoRepository.save(notificacao(7L, "estoque", "inapp", false, "Estoque baixo"));
        notificacaoRepository.save(notificacao(7L, "pessoas", "inapp", true, "Aviso lido"));

        ResponseEntity<String> porTipo = restTemplate.exchange("/notifications?type=estoque",
                HttpMethod.GET, new HttpEntity<>(null, bearer(7L, "BOLSISTA")), String.class);
        assertThat(porTipo.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(porTipo.getBody()).contains("\"total\":1");

        ResponseEntity<String> naoLidas = restTemplate.exchange("/notifications?read=false",
                HttpMethod.GET, new HttpEntity<>(null, bearer(7L, "BOLSISTA")), String.class);
        assertThat(naoLidas.getBody()).contains("\"total\":1");
    }

    @Test
    void marcarComoLidaRespeitaPosse() {
        Notificacao minha = notificacaoRepository.save(notificacao(7L, "sistema", "inapp", false, "Minhas"));

        ResponseEntity<String> ok = restTemplate.exchange("/notifications/" + minha.getId() + "/read",
                HttpMethod.PATCH, new HttpEntity<>(null, bearer(7L, "BOLSISTA")), String.class);
        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ok.getBody()).contains("\"read\":true");

        ResponseEntity<String> alheia = restTemplate.exchange("/notifications/" + minha.getId() + "/read",
                HttpMethod.PATCH, new HttpEntity<>(null, bearer(8L, "BOLSISTA")), String.class);
        assertThat(alheia.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> inexistente = restTemplate.exchange("/notifications/999999/read",
                HttpMethod.PATCH, new HttpEntity<>(null, bearer(7L, "BOLSISTA")), String.class);
        assertThat(inexistente.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void marcarTodasComoLidasAfetaSomenteOUsuario() {
        notificacaoRepository.save(notificacao(7L, "sistema", "inapp", false, "Uma"));
        notificacaoRepository.save(notificacao(7L, "sistema", "inapp", false, "Duas"));
        notificacaoRepository.save(notificacao(8L, "sistema", "inapp", false, "Outro"));

        ResponseEntity<String> response = restTemplate.exchange("/notifications/read-all",
                HttpMethod.POST, new HttpEntity<>(null, bearer(7L, "BOLSISTA")), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"lidas\":2");

        ResponseEntity<String> contagem = restTemplate.exchange("/notifications/unread/count",
                HttpMethod.GET, new HttpEntity<>(null, bearer(8L, "BOLSISTA")), String.class);
        assertThat(contagem.getBody()).contains("\"count\":1");
    }

    @Test
    void preferenciasExigemAdmin() {
        ResponseEntity<String> negado = restTemplate.exchange("/notifications/preferences",
                HttpMethod.GET, new HttpEntity<>(null, bearer(7L, "BOLSISTA")), String.class);
        assertThat(negado.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> admin = restTemplate.exchange("/notifications/preferences",
                HttpMethod.GET, new HttpEntity<>(null, bearer(1L, "ADMIN")), String.class);
        assertThat(admin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(admin.getBody()).contains("\"sistema\"");
    }

    @Test
    void salvarPreferenciasValidaMatriz() {
        HttpHeaders headers = bearer(1L, "ADMIN");
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> invalida = restTemplate.exchange("/notifications/preferences",
                HttpMethod.PUT, new HttpEntity<>("{\"tipo-inexistente\":{\"inapp\":true}}", headers),
                String.class);
        assertThat(invalida.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalida.getBody()).doesNotContain("Exception");

        ResponseEntity<String> valida = restTemplate.exchange("/notifications/preferences",
                HttpMethod.PUT,
                new HttpEntity<>("{\"sistema\":{\"inapp\":true,\"email\":false,\"push\":false}}", headers),
                String.class);
        assertThat(valida.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(valida.getBody()).contains("\"email\":false");
    }

    @Test
    void historicoExigeAdminRetornaListaCompletaEFiltra() {
        notificacaoRepository.save(notificacao(7L, "estoque", "email", false, "Estoque baixo item X"));
        notificacaoRepository.save(notificacao(8L, "pessoas", "inapp", true, "Hora validada"));
        Notificacao antiga = notificacao(9L, "estoque", "inapp", false, "Antiga");
        antiga.setCriadaEm(Instant.now().minusSeconds(60L * 86400));
        notificacaoRepository.save(antiga);

        ResponseEntity<String> negado = restTemplate.exchange("/notifications/history",
                HttpMethod.GET, new HttpEntity<>(null, bearer(7L, "BOLSISTA")), String.class);
        assertThat(negado.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> completo = restTemplate.exchange("/notifications/history",
                HttpMethod.GET, new HttpEntity<>(null, bearer(1L, "ADMIN")), String.class);
        assertThat(completo.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(completo.getBody()).contains("\"total\":3");
        assertThat(completo.getBody()).contains("\"items\"");

        ResponseEntity<String> busca = restTemplate.exchange(
                org.springframework.web.util.UriComponentsBuilder.fromPath("/notifications/history")
                        .queryParam("search", "item x").encode().build().toUri(),
                HttpMethod.GET, new HttpEntity<>(null, bearer(1L, "ADMIN")), String.class);
        assertThat(busca.getBody()).contains("\"total\":1");

        ResponseEntity<String> recentes = restTemplate.exchange("/notifications/history?period=30d",
                HttpMethod.GET, new HttpEntity<>(null, bearer(1L, "ADMIN")), String.class);
        assertThat(recentes.getBody()).contains("\"total\":2");

        ResponseEntity<String> periodoInvalido = restTemplate.exchange("/notifications/history?period=ontem",
                HttpMethod.GET, new HttpEntity<>(null, bearer(1L, "ADMIN")), String.class);
        assertThat(periodoInvalido.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rotasExigemAutenticacao() {
        assertThat(restTemplate.getForEntity("/notifications", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
