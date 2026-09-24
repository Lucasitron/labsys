package com.fablab.auth.controller;

import com.fablab.auth.dto.LoginPrincipal;
import com.fablab.auth.dto.LoginRequest;
import com.fablab.auth.dto.LoginResponse;
import com.fablab.auth.dto.LogoutRequest;
import com.fablab.auth.dto.LogoutResponse;
import com.fablab.auth.dto.MeResponse;
import com.fablab.auth.dto.RbacResponse;
import com.fablab.auth.dto.RefreshRequest;
import com.fablab.auth.dto.RefreshResponse;
import com.fablab.auth.dto.ValidateRfidRequest;
import com.fablab.auth.dto.ValidateRfidResponse;
import com.fablab.auth.entity.Role;
import com.fablab.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints públicos e protegidos do Auth &amp; Identity Service.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody(required = false) LogoutRequest request) {
        String token = (request != null && !isBlank(request.token()))
                ? request.token()
                : extractBearer(authorization);
        authService.logout(token);
        return ResponseEntity.ok(new LogoutResponse("Logout realizado com sucesso"));
    }

    @PostMapping("/validate-rfid")
    public ResponseEntity<ValidateRfidResponse> validateRfid(@Valid @RequestBody ValidateRfidRequest request) {
        return ResponseEntity.ok(authService.validateRfid(request));
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        LoginPrincipal principal = (LoginPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(authService.me(principal));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @GetMapping("/permissions")
    public ResponseEntity<RbacResponse> permissions(
            @RequestParam(defaultValue = "ADMIN") Role role) {
        return ResponseEntity.ok(authService.permissions(role));
    }

    private String extractBearer(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring("Bearer ".length());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}