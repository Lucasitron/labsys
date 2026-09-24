package com.fablab.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.auth.dto.LoginPrincipal;
import com.fablab.auth.dto.LoginRequest;
import com.fablab.auth.dto.LoginResponse;
import com.fablab.auth.dto.MeResponse;
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
import com.fablab.auth.service.AccessLogService;
import com.fablab.auth.service.AuthService;
import com.fablab.auth.service.JwtService;
import com.fablab.auth.service.RbacService;
import com.fablab.auth.service.RfidEventPublisher;
import com.fablab.auth.service.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private LoginRepository loginRepository;
    @Mock
    private UserPermissionRepository permissionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private TokenBlacklistService blacklistService;
    @Mock
    private AccessLogService accessLogService;
    @Mock
    private RfidEventPublisher eventPublisher;
    @Mock
    private RbacService rbacService;

    private AuthService authService;
    private Login admin;
    private UserPermission adminPermission;

    @BeforeEach
    void setUp() {
        authService = new AuthService(loginRepository, permissionRepository, passwordEncoder, jwtService,
                blacklistService, accessLogService, eventPublisher, rbacService);

        admin = TestLogin.login(1L, 7L, "CARD-001", "admin@fablab.io", "admin", "Direção");
        admin.setSenhaHash("$2a$10$hashed");
        adminPermission = TestLogin.permission(7L, Role.ADMIN, true);

        lenient().when(jwtService.generateAccessToken(any(), any())).thenReturn("access-token");
        lenient().when(jwtService.generateRefreshToken(any(), any())).thenReturn("refresh-token");
        lenient().when(jwtService.accessExpirationSeconds()).thenReturn(900L);
    }

    @Test
    void loginByEmailReturnsTokenPair() {
        when(loginRepository.findByEmail("admin@fablab.io")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("Senha@123", admin.getSenhaHash())).thenReturn(true);
        when(permissionRepository.findFirstByIdUserAndActiveTrue(7L))
                .thenReturn(Optional.of(adminPermission));

        LoginResponse response = authService.login(
                new LoginRequest("admin@fablab.io", null, "Senha@123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.idUser()).isEqualTo(7L);
        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(response.setor()).isEqualTo("Direção");
        verify(jwtService).generateAccessToken(admin, Role.ADMIN);
    }

    @Test
    void loginByNomeUsuarioReturnsTokenPair() {
        when(loginRepository.findByNomeUsuario("admin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("Senha@123", admin.getSenhaHash())).thenReturn(true);
        when(permissionRepository.findFirstByIdUserAndActiveTrue(7L))
                .thenReturn(Optional.of(adminPermission));

        LoginResponse response = authService.login(
                new LoginRequest(null, "admin", "Senha@123"));

        assertThat(response.nomeUsuario()).isEqualTo("admin");
    }

    @Test
    void loginWithWrongPasswordThrows() {
        when(loginRepository.findByEmail("admin@fablab.io")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("errada", admin.getSenhaHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("admin@fablab.io", null, "errada")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginWithUnknownUserThrows() {
        when(loginRepository.findByEmail("ghost@fablab.io")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("ghost@fablab.io", null, "Qualquer123")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginWithoutActivePermissionThrows() {
        when(loginRepository.findByEmail("admin@fablab.io")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("Senha@123", admin.getSenhaHash())).thenReturn(true);
        when(permissionRepository.findFirstByIdUserAndActiveTrue(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("admin@fablab.io", null, "Senha@123")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void loginWithoutIdentifierThrows() {
        assertThatThrownBy(() -> authService.login(new LoginRequest("", "", "Senha@123")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void logoutBlacklistsCurrentToken() {
        Claims claims = TestLogin.claims("1", Date.from(Instant.now().plusSeconds(900)));
        lenient().when(jwtService.parseClaims("token")).thenReturn(claims);
        when(blacklistService.isBlacklisted("token")).thenReturn(false);

        authService.logout("token");

        verify(blacklistService).blacklist("token", claims);
    }

    @Test
    void logoutWithBlankTokenThrows() {
        assertThatThrownBy(() -> authService.logout("  "))
                .isInstanceOf(TokenInvalidException.class);
    }

    @Test
    void logoutWithMalformedTokenThrows() {
        when(jwtService.parseClaims("garbage")).thenThrow(new io.jsonwebtoken.JwtException("bad"));

        assertThatThrownBy(() -> authService.logout("garbage"))
                .isInstanceOf(TokenInvalidException.class);
    }

    @Test
    void refreshRotatesTokens() {
        Claims claims = TestLogin.claims("1", Date.from(Instant.now().plusSeconds(604800)));
        when(jwtService.parseClaims("refresh-token-old")).thenReturn(claims);
        when(jwtService.isRefreshToken(claims)).thenReturn(true);
        when(blacklistService.isBlacklisted("refresh-token-old")).thenReturn(false);
        when(loginRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(permissionRepository.findFirstByIdUserAndActiveTrue(7L))
                .thenReturn(Optional.of(adminPermission));

        RefreshResponse response = authService.refresh(new RefreshRequest("refresh-token-old"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(blacklistService).blacklist("refresh-token-old", claims);
    }

    @Test
    void refreshWithAccessTokenThrows() {
        when(jwtService.isRefreshToken(any())).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("access-token")))
                .isInstanceOf(TokenInvalidException.class);
    }

    @Test
    void refreshWithBlacklistedTokenThrows() {
        when(jwtService.isRefreshToken(any())).thenReturn(true);
        when(blacklistService.isBlacklisted("refresh-token-old")).thenReturn(true);

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("refresh-token-old")))
                .isInstanceOf(TokenBlacklistedException.class);
    }

    @Test
    void refreshWithUnknownLoginThrows() {
        Claims claims = TestLogin.claims("999", Date.from(Instant.now().plusSeconds(604800)));
        when(jwtService.parseClaims("refresh-token-old")).thenReturn(claims);
        when(jwtService.isRefreshToken(claims)).thenReturn(true);
        when(blacklistService.isBlacklisted("refresh-token-old")).thenReturn(false);
        when(loginRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("refresh-token-old")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void meReturnsUserDetailsAndPermissions() {
        LoginPrincipal principal = new LoginPrincipal(1L, 7L, Role.ADMIN, "Direção");
        when(loginRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(permissionRepository.findByIdUser(7L))
                .thenReturn(List.of(TestLogin.permission(7L, Role.ADMIN, true)));

        MeResponse response = authService.me(principal);

        assertThat(response.idUser()).isEqualTo(7L);
        assertThat(response.email()).isEqualTo("admin@fablab.io");
        assertThat(response.permissions()).hasSize(1);
        assertThat(response.permissions().get(0).role()).isEqualTo("ADMIN");
        assertThat(LoginMapper.toMeResponse(admin, List.of(adminPermission)).permissions().get(0).label())
                .isEqualTo("Admin");
    }

    @Test
    void validateRfidKnownCardPublishesEvent() {
        ValidateRfidRequest request = new ValidateRfidRequest("CARD-001");
        when(loginRepository.findByUuid("CARD-001")).thenReturn(Optional.of(admin));
        when(accessLogService.resolveType("CARD-001")).thenReturn(AccessLogType.ENTRADA);
        AccessLog entry = new AccessLog();
        entry.setIdUser(7L);
        entry.setTimestamp(Instant.parse("2026-01-01T10:00:00Z"));
        entry.setType(AccessLogType.ENTRADA);
        when(accessLogService.record(7L, "CARD-001", AccessLogType.ENTRADA)).thenReturn(entry);

        ValidateRfidResponse response = authService.validateRfid(request);

        assertThat(response.allowed()).isTrue();
        assertThat(response.idUser()).isEqualTo(7L);
        assertThat(response.type()).isEqualTo("ENTRADA");
        ArgumentCaptor<RfidAccessEvent> captor = ArgumentCaptor.forClass(RfidAccessEvent.class);
        verify(eventPublisher).publishAccess(captor.capture());
        assertThat(captor.getValue().idUser()).isEqualTo(7L);
        assertThat(captor.getValue().type()).isEqualTo(AccessLogType.ENTRADA);
    }

    @Test
    void validateRfidTogglesToSaidaAndPublishes() {
        ValidateRfidRequest request = new ValidateRfidRequest("CARD-001");
        when(loginRepository.findByUuid("CARD-001")).thenReturn(Optional.of(admin));
        when(accessLogService.resolveType("CARD-001")).thenReturn(AccessLogType.SAIDA);
        AccessLog entry = new AccessLog();
        entry.setIdUser(7L);
        entry.setTimestamp(Instant.parse("2026-01-01T18:00:00Z"));
        entry.setType(AccessLogType.SAIDA);
        when(accessLogService.record(7L, "CARD-001", AccessLogType.SAIDA)).thenReturn(entry);

        ValidateRfidResponse response = authService.validateRfid(request);

        assertThat(response.type()).isEqualTo("SAIDA");
        verify(eventPublisher).publishAccess(any(RfidAccessEvent.class));
    }

    @Test
    void validateRfidUnknownCardLogsDeniedAndDoesNotPublish() {
        ValidateRfidRequest request = new ValidateRfidRequest("CARD-UNKNOWN");
        when(loginRepository.findByUuid("CARD-UNKNOWN")).thenReturn(Optional.empty());
        AccessLog denied = new AccessLog();
        denied.setUuidRfid("CARD-UNKNOWN");
        denied.setType(AccessLogType.ACESSO_NEGADO);
        denied.setTimestamp(Instant.parse("2026-01-01T09:00:00Z"));
        when(accessLogService.record(eq(null), eq("CARD-UNKNOWN"), eq(AccessLogType.ACESSO_NEGADO)))
                .thenReturn(denied);

        ValidateRfidResponse response = authService.validateRfid(request);

        assertThat(response.allowed()).isFalse();
        assertThat(response.type()).isEqualTo("ACESSO_NEGADO");
        assertThat(response.idUser()).isNull();
        verify(eventPublisher, never()).publishAccess(any());
    }

    @Test
    void permissionsDelegatesToRbacService() {
        when(rbacService.getMatrix(Role.BOLSISTA))
                .thenReturn(new RbacResponse("BOLSISTA", 1, "Bolsista", List.of("catalogo:read")));

        RbacResponse response = authService.permissions(Role.BOLSISTA);

        assertThat(response.role()).isEqualTo("BOLSISTA");
        verify(rbacService).getMatrix(Role.BOLSISTA);
    }

    private static final class TestLogin {
        private TestLogin() {
        }

        static Login login(long id, long idUser, String uuid, String email, String nome, String setor) {
            Login login = new Login();
            login.setId(id);
            login.setIdUser(idUser);
            login.setUuid(uuid);
            login.setEmail(email);
            login.setNomeUsuario(nome);
            login.setSetor(setor);
            return login;
        }

        static UserPermission permission(long idUser, Role role, boolean active) {
            UserPermission permission = new UserPermission();
            permission.setIdUser(idUser);
            permission.setRole(role);
            permission.setActive(active);
            return permission;
        }

        static Claims claims(String subject, Date expiration) {
            Claims claims = org.mockito.Mockito.mock(Claims.class);
            when(claims.getSubject()).thenReturn(subject);
            lenient().when(claims.getExpiration()).thenReturn(expiration);
            return claims;
        }
    }
}