package com.fablab.rh.config;

import com.fablab.rh.dto.RhPrincipal;
import com.fablab.rh.entity.NivelAcesso;
import com.fablab.rh.repository.FuncionarioRepository;
import com.fablab.rh.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Intercepta as requisições, valida o JWT do Auth Service e popula o contexto
 * de autenticação do Spring Security.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final FuncionarioRepository funcionarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, FuncionarioRepository funcionarioRepository) {
        this.jwtService = jwtService;
        this.funcionarioRepository = funcionarioRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().equals("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            Claims claims = jwtService.parseClaims(token);
            Long idPessoa = jwtService.extractIdUser(claims);
            NivelAcesso nivel = NivelAcesso.valueOf(jwtService.extractRole(claims));

            Optional<Long> idFuncionario =
                    funcionarioRepository.findByPessoaId(idPessoa).map(f -> f.getId());

            RhPrincipal principal = new RhPrincipal(
                    idPessoa, idFuncionario.orElse(null), nivel, jwtService.extractSetor(claims));

            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(
                            principal, null, principal.authorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}