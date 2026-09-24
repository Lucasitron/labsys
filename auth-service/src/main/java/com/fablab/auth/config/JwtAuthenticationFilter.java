package com.fablab.auth.config;

import com.fablab.auth.dto.LoginPrincipal;
import com.fablab.auth.service.JwtService;
import com.fablab.auth.service.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Intercepta as requisições, valida o token JWT e popula o contexto de
 * autenticação do Spring Security.
 *
 * <p>Tokens revogados (blacklist) ou do tipo {@code refresh} não autenticam.</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklistService blacklistService;

    public JwtAuthenticationFilter(JwtService jwtService, TokenBlacklistService blacklistService) {
        this.jwtService = jwtService;
        this.blacklistService = blacklistService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/auth/login")
                || path.equals("/auth/validate-rfid")
                || path.equals("/auth/refresh");
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
            if (blacklistService.isBlacklisted(token)) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }
            Claims claims = jwtService.parseClaims(token);
            if (jwtService.isRefreshToken(claims)) {
                filterChain.doFilter(request, response);
                return;
            }

            LoginPrincipal principal = new LoginPrincipal(
                    Long.valueOf(claims.getSubject()),
                    jwtService.extractIdUser(claims),
                    jwtService.extractRole(claims),
                    jwtService.extractSetor(claims));

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