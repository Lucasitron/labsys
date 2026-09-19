package com.fablab.auth.service;

import com.fablab.auth.dto.LoginRequest;
import com.fablab.auth.dto.LoginResponse;
import com.fablab.auth.dto.UserResponse;
import com.fablab.auth.entity.Responsabilidade;
import com.fablab.auth.entity.Usuario;
import com.fablab.auth.exception.ContaBloqueadaException;
import com.fablab.auth.exception.CredenciaisInvalidasException;
import com.fablab.auth.exception.ResourceNotFoundException;
import com.fablab.auth.repository.ResponsabilidadeRepository;
import com.fablab.auth.repository.UsuarioRepository;
import com.fablab.auth.config.JwtProperties;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autenticação e consulta de perfil do usuário.
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ResponsabilidadeRepository responsabilidadeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          ResponsabilidadeRepository responsabilidadeRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          JwtProperties jwtProperties) {
        this.usuarioRepository = usuarioRepository;
        this.responsabilidadeRepository = responsabilidadeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional(readOnly = true)
    public LoginResponse autenticar(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByUsername(request.username().trim())
                .orElseThrow(() -> new CredenciaisInvalidasException("Usuário ou senha inválidos"));

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new CredenciaisInvalidasException("Usuário ou senha inválidos");
        }
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new ContaBloqueadaException("Conta bloqueada. Contate o administrador.");
        }

        return new LoginResponse(
                jwtService.emitir(usuario),
                jwtProperties.expirationSeconds(),
                UserResponse.of(usuario, responsabilidades(usuario.getId())));
    }

    @Transactional(readOnly = true)
    public UserResponse carregarPerfil(Long idUser) {
        Usuario usuario = usuarioRepository.findById(idUser)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + idUser));
        return UserResponse.of(usuario, responsabilidades(usuario.getId()));
    }

    private Map<String, List<String>> responsabilidades(Long idUsuario) {
        Map<String, List<String>> porModulo = new LinkedHashMap<>();
        for (Responsabilidade responsabilidade : responsabilidadeRepository.findByUsuarioId(idUsuario)) {
            porModulo.computeIfAbsent(responsabilidade.getModulo(), m -> new java.util.ArrayList<>())
                    .add(responsabilidade.getRecurso());
        }
        return porModulo;
    }
}