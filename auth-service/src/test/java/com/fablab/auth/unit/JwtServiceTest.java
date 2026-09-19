package com.fablab.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fablab.auth.config.JwtProperties;
import com.fablab.auth.entity.Usuario;
import com.fablab.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

/**
 * Emissão e validação dos tokens JWT.
 */
class JwtServiceTest {

    private JwtService service() {
        return new JwtService(new JwtProperties("minha-chave-secreta-32-characters-long-teste", 3600, "fablab-test"));
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setUsername("joao.silva");
        usuario.setNomeCompleto("João Silva");
        usuario.setEmail("joao@fablab.org");
        usuario.setRole(1);
        usuario.setSetor("marketing");
        usuario.setAtivo(true);
        return usuario;
    }

    @Test
    void emiteTokenComClaimsEsperados() {
        JwtService jwtService = service();
        String token = jwtService.emitir(usuario());

        Claims claims = jwtService.parseClaims(token);
        assertThat(claims.getSubject()).isEqualTo("joao.silva");
        assertThat(((Number) claims.get(JwtService.CLAIM_ID_USER)).longValue()).isEqualTo(7L);
        assertThat(claims.get(JwtService.CLAIM_ROLE, String.class)).isEqualTo("BOLSISTA");
        assertThat(claims.get(JwtService.CLAIM_SETOR, String.class)).isEqualTo("marketing");
        assertThat(claims.getIssuer()).isEqualTo("fablab-test");
        assertThat(claims.getExpiration()).isNotNull();
    }

    @Test
    void rejeitaTokenComOutroEmissor() {
        JwtProperties props = new JwtProperties("minha-chave-secreta-32-characters-long-teste", 3600, "fablab-test");
        String token = service().emitir(usuario());

        JwtService outroEmissor = new JwtService(new JwtProperties(props.secret(), props.expirationSeconds(), "outro"));
        assertThatThrownBy(() -> outroEmissor.parseClaims(token))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    @Test
    void exigeChaveDePeloMenos32Caracteres() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("curta", 3600, "fablab-test")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void roleNameMapecoNiveis() {
        assertThat(JwtService.roleName(0)).isEqualTo("ADMIN");
        assertThat(JwtService.roleName(4)).isEqualTo("RECRUTANDO");
    }

    @Test
    void roleNameInvalidoLancaErro() {
        assertThatThrownBy(() -> JwtService.roleName(9)).isInstanceOf(IllegalArgumentException.class);
    }
}