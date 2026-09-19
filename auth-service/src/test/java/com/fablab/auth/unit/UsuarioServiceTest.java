package com.fablab.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fablab.auth.config.JwtProperties;
import com.fablab.auth.dto.LoginRequest;
import com.fablab.auth.dto.LoginResponse;
import com.fablab.auth.entity.Responsabilidade;
import com.fablab.auth.entity.Usuario;
import com.fablab.auth.exception.ContaBloqueadaException;
import com.fablab.auth.exception.CredenciaisInvalidasException;
import com.fablab.auth.exception.ResourceNotFoundException;
import com.fablab.auth.repository.ResponsabilidadeRepository;
import com.fablab.auth.repository.UsuarioRepository;
import com.fablab.auth.service.JwtService;
import com.fablab.auth.service.UsuarioService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Autenticação e perfil (usuário ativo, credenciais, bloqueio, responsabilidades).
 */
class UsuarioServiceTest {

    private UsuarioRepository usuarioRepository;
    private ResponsabilidadeRepository responsabilidadeRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private UsuarioService service;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        responsabilidadeRepository = mock(ResponsabilidadeRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        service = new UsuarioService(usuarioRepository, responsabilidadeRepository,
                passwordEncoder, jwtService, new JwtProperties("x", 3600, "fablab-test"));
    }

    private Usuario usuarioAtivo() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("admin");
        usuario.setPasswordHash("hash");
        usuario.setNomeCompleto("Admin FabLab");
        usuario.setEmail("admin@fablab.org");
        usuario.setRole(0);
        usuario.setAtivo(true);
        return usuario;
    }

    @Test
    void autenticaComCredenciaisValidas() {
        Usuario usuario = usuarioAtivo();
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("admin123", "hash")).thenReturn(true);
        when(jwtService.emitir(usuario)).thenReturn("token-abc");

        LoginResponse response = service.autenticar(new LoginRequest("admin", "admin123"));

        assertThat(response.token()).isEqualTo("token-abc");
        assertThat(response.expiresIn()).isEqualTo(3600);
        assertThat(response.user().username()).isEqualTo("admin");
        assertThat(response.user().role()).isZero();
        assertThat(response.user().roles().get("financeiro")).isEqualTo("edit");
    }

    @Test
    void rejeitaUsuarioInexistente() {
        when(usuarioRepository.findByUsername("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.autenticar(new LoginRequest("nope", "x")))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    void rejeitaSenhaIncorreta() {
        Usuario usuario = usuarioAtivo();
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        assertThatThrownBy(() -> service.autenticar(new LoginRequest("admin", "errada")))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    void bloqueiaContaInativa() {
        Usuario usuario = usuarioAtivo();
        usuario.setAtivo(false);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        assertThatThrownBy(() -> service.autenticar(new LoginRequest("admin", "admin123")))
                .isInstanceOf(ContaBloqueadaException.class);
    }

    @Test
    void carregaPerfilComResponsabilidades() {
        Usuario usuario = usuarioAtivo();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        Responsabilidade resp = new Responsabilidade();
        resp.setModulo("producao");
        resp.setRecurso("impressora_3d");
        when(responsabilidadeRepository.findByUsuarioId(1L)).thenReturn(List.of(resp));

        var perfil = service.carregarPerfil(1L);
        assertThat(perfil.name()).isEqualTo("Admin FabLab");
        assertThat(perfil.responsibilities().get("producao")).containsExactly("impressora_3d");
    }

    @Test
    void perfilInexistenteLanca404() {
        when(usuarioRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.carregarPerfil(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}