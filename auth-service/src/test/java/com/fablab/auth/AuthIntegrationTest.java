package com.fablab.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.fablab.auth.dto.LoginResponse;
import com.fablab.auth.dto.UserResponse;
import com.fablab.auth.entity.Usuario;
import com.fablab.auth.repository.UsuarioRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
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
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Fluxo login → /auth/me pelo HTTP real (filtro de segurança incluído).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @BeforeEach
    void seedAdmin() {
        usuarioRepository.deleteAll();
        usuarioRepository.save(usuario("admin", "admin123", "Admin FabLab", "admin@fablab.org", 0, true));
    }

    private Usuario usuario(String username, String password, String name, String email, int role, boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setNomeCompleto(name);
        usuario.setEmail(email);
        usuario.setRole(role);
        usuario.setSetor("geral");
        usuario.setAtivo(ativo);
        return usuario;
    }

    private ResponseEntity<LoginResponse> login(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        return restTemplate.postForEntity("/auth/login", new HttpEntity<>(body, headers), LoginResponse.class);
    }

    private ResponseEntity<String> loginRaw(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        return restTemplate.postForEntity("/auth/login", new HttpEntity<>(body, headers), String.class);
    }

    @Test
    void loginValidoRetornaTokenEPerfil() {
        ResponseEntity<LoginResponse> response = login("admin", "admin123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isNotBlank();
        assertThat(response.getBody().user().username()).isEqualTo("admin");
        assertThat(response.getBody().user().role()).isZero();
        assertThat(response.getBody().user().roles().get("rh")).isEqualTo("edit");
        assertThat(response.getBody().user().roles().get("financeiro")).isEqualTo("edit");
    }

    @Test
    void loginInvalidoRetorna401() {
        ResponseEntity<String> response = loginRaw("admin", "senha-errada");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).contains("Usuário ou senha inválidos");
    }

    @Test
    void meRetornaUsuarioAutenticado() {
        ResponseEntity<LoginResponse> loginResponse = login("admin", "admin123");
        String token = loginResponse.getBody().token();

        ResponseEntity<UserResponse> me = restTemplate.exchange("/auth/me", HttpMethod.GET,
                new HttpEntity<>(null, bearer(token)), UserResponse.class);

        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody()).isNotNull();
        assertThat(me.getBody().name()).isEqualTo("Admin FabLab");
    }

    @Test
    void meSemTokenRetorna401() {
        ResponseEntity<String> me = restTemplate.getForEntity("/auth/me", String.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(me.getBody()).contains("Autenticação necessária");
    }

    @Test
    void meComIdDesconhecidoRetorna404() {
        String token = tokenParaUsuario(999999L, "ghost");

        ResponseEntity<String> me = restTemplate.exchange("/auth/me", HttpMethod.GET,
                new HttpEntity<>(null, bearer(token)), String.class);

        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(me.getBody()).contains("Usuário não encontrado");
    }

    @Test
    void contaInativaRetorna423() {
        usuarioRepository.save(usuario("bloqueado", "senha-bloco", "Bloqueado", "bloqueado@fablab.org", 3, false));

        ResponseEntity<LoginResponse> response = login("bloqueado", "senha-bloco");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
    }

    @Test
    void loginSemCorpoRetorna400() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.postForEntity("/auth/login",
                new HttpEntity<>("{}", headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private String tokenParaUsuario(long id, String username) {
        long agora = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .claims(Map.of(
                        "id_user", id,
                        "role", "ESTAGIARIO",
                        "setor", "geral"))
                .issuer("fablab-test")
                .issuedAt(new java.util.Date(agora))
                .expiration(new java.util.Date(agora + 3600_000))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}