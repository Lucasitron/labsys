package com.fablab.auth.service;

import com.fablab.auth.dto.LoginPrincipal;
import com.fablab.auth.dto.LoginRequest;
import com.fablab.auth.dto.LoginResponse;
import com.fablab.auth.dto.MeResponse;
import com.fablab.auth.dto.PermissionDto;
import com.fablab.auth.dto.RbacResponse;
import com.fablab.auth.dto.RefreshRequest;
import com.fablab.auth.dto.RefreshResponse;
import com.fablab.auth.dto.RfidAccessEvent;
import com.fablab.auth.dto.ValidateRfidRequest;
import com.fablab.auth.dto.ValidateRfidResponse;
import com.fablab.auth.entity.AccessLog;
import com.fablab.auth.entity.AccessLogType;
import com.fablab.auth.entity.Login;
import com.fablab.auth.entity.Role;
import com.fablab.auth.entity.UserPermission;
import com.fablab.auth.exception.ForbiddenException;
import com.fablab.auth.exception.InvalidCredentialsException;
import com.fablab.auth.exception.ResourceNotFoundException;
import com.fablab.auth.exception.TokenBlacklistedException;
import com.fablab.auth.exception.TokenInvalidException;
import com.fablab.auth.mapper.LoginMapper;
import com.fablab.auth.repository.LoginRepository;
import com.fablab.auth.repository.UserPermissionRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestra os fluxos de autenticação, autorização e validação RFID.
 */
@Service
public class AuthService {

    private final LoginRepository loginRepository;
    private final UserPermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService blacklistService;
    private final AccessLogService accessLogService;
    private final RfidEventPublisher eventPublisher;
    private final RbacService rbacService;

    public AuthService(LoginRepository loginRepository,
                       UserPermissionRepository permissionRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       TokenBlacklistService blacklistService,
                       AccessLogService accessLogService,
                       RfidEventPublisher eventPublisher,
                       RbacService rbacService) {
        this.loginRepository = loginRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.blacklistService = blacklistService;
        this.accessLogService = accessLogService;
        this.eventPublisher = eventPublisher;
        this.rbacService = rbacService;
    }

    /**
     * Autentica via e-mail/nome de usuário e senha, retornando o par de tokens.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        if (isBlank(request.email()) && isBlank(request.nomeUsuario())) {
            throw new IllegalArgumentException("Informe e-mail ou nome de usuário");
        }

        Login login = isBlank(request.email())
                ? loginRepository.findByNomeUsuario(request.nomeUsuario()).orElse(null)
                : loginRepository.findByEmail(request.email()).orElse(null);

        if (login == null || !passwordEncoder.matches(request.senha(), login.getSenhaHash())) {
            throw new InvalidCredentialsException();
        }

        Role role = activeRole(login);
        return new LoginResponse(
                jwtService.generateAccessToken(login, role),
                jwtService.generateRefreshToken(login, role),
                "Bearer",
                jwtService.accessExpirationSeconds(),
                login.getIdUser(),
                role.name(),
                login.getSetor(),
                login.getNomeUsuario());
    }

    /**
     * Revoga o token JWT atual (adiciona à blacklist).
     */
    @Transactional
    public void logout(String token) {
        if (isBlank(token)) {
            throw new TokenInvalidException();
        }
        Claims claims = parse(token);
        blacklistService.blacklist(token, claims);
    }

    /**
     * Renova o par de tokens usando o refresh token, com rotação (o refresh
     * token antigo é invalidado).
     */
    @Transactional
    public RefreshResponse refresh(RefreshRequest request) {
        Claims claims = parse(request.refreshToken());
        if (!jwtService.isRefreshToken(claims)) {
            throw new TokenInvalidException();
        }
        if (blacklistService.isBlacklisted(request.refreshToken())) {
            throw new TokenBlacklistedException();
        }

        long loginId = Long.parseLong(claims.getSubject());
        Login login = loginRepository.findById(loginId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        Role role = activeRole(login);

        blacklistService.blacklist(request.refreshToken(), claims);
        return new RefreshResponse(
                jwtService.generateAccessToken(login, role),
                jwtService.generateRefreshToken(login, role),
                "Bearer",
                jwtService.accessExpirationSeconds());
    }

    /**
     * Retorna os dados do usuário e suas permissões.
     */
    @Transactional(readOnly = true)
    public MeResponse me(LoginPrincipal principal) {
        Login login = loginRepository.findById(principal.loginId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        List<UserPermission> permissions = permissionRepository.findByIdUser(login.getIdUser());
        return LoginMapper.toMeResponse(login, permissions);
    }

    /**
     * Valida o UUID do cartão RFID, registra o acesso e publica o evento de ponto.
     */
    @Transactional
    public ValidateRfidResponse validateRfid(ValidateRfidRequest request) {
        Login login = loginRepository.findByUuid(request.uuid()).orElse(null);

        if (login == null) {
            AccessLog denied = accessLogService.record(null, request.uuid(), AccessLogType.ACESSO_NEGADO);
            return new ValidateRfidResponse(
                    false,
                    "Cartão RFID não reconhecido",
                    null,
                    request.uuid(),
                    null,
                    null,
                    denied.getType().name(),
                    denied.getTimestamp());
        }

        AccessLogType type = accessLogService.resolveType(request.uuid());
        AccessLog entry = accessLogService.record(login.getIdUser(), request.uuid(), type);
        eventPublisher.publishAccess(new RfidAccessEvent(
                login.getIdUser(), request.uuid(), entry.getTimestamp(), type));

        return new ValidateRfidResponse(
                true,
                "Acesso registrado",
                login.getIdUser(),
                request.uuid(),
                login.getNomeUsuario(),
                login.getSetor(),
                type.name(),
                entry.getTimestamp());
    }

    /**
     * Retorna a matriz RBAC de um papel.
     */
    public RbacResponse permissions(Role role) {
        return rbacService.getMatrix(role);
    }

    private Role activeRole(Login login) {
        return permissionRepository.findFirstByIdUserAndActiveTrue(login.getIdUser())
                .map(UserPermission::getRole)
                .orElseThrow(() -> new ForbiddenException("Usuário não possui permissão ativa"));
    }

    private Claims parse(String token) {
        try {
            return jwtService.parseClaims(token);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new TokenInvalidException();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}