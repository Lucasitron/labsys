package com.fablab.auth.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.auth.entity.Login;
import com.fablab.auth.entity.Role;
import com.fablab.auth.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SecurityConfigTest extends BaseIntegrationTest {

    private Login admin;
    private Login bolsista;

    @BeforeEach
    void seed() {
        admin = seedUser(100L, Role.ADMIN, "CARD-ADMIN", "admin@fablab.io", "admin", "Direção");
        bolsista = seedUser(101L, Role.BOLSISTA, "CARD-BOLSISTA", "bolsista@fablab.io", "bolsista", "Laboratório");
    }

    @Test
    void anonymousRequestToProtectedEndpointIsRejected() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void anonymousRequestToPermissionsIsRejected() throws Exception {
        mockMvc.perform(get("/auth/permissions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanReadPermissionMatrix() throws Exception {
        String token = accessToken(admin, Role.ADMIN);

        mockMvc.perform(get("/auth/permissions?role=ADMIN")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.permissions[0]").value("*"));
    }

    @Test
    void nonAdminIsDeniedPermissionMatrix() throws Exception {
        String token = accessToken(bolsista, Role.BOLSISTA);

        mockMvc.perform(get("/auth/permissions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void loginEndpointIsPublic() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"admin@fablab.io","senha":"%s"}
                                """.formatted(SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void validTokenAllowsAccessToProtectedEndpoint() throws Exception {
        String token = accessToken(admin, Role.ADMIN);

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void invalidTokenIsRejected() throws Exception {
        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() throws Exception {
        String refresh = refreshToken(admin, Role.ADMIN);

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + refresh))
                .andExpect(status().isUnauthorized());
    }
}