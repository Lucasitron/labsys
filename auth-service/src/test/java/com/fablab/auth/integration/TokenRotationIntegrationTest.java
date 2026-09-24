package com.fablab.auth.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.auth.entity.Login;
import com.fablab.auth.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TokenRotationIntegrationTest extends BaseIntegrationTest {

    private Login admin;

    @BeforeEach
    void seed() {
        admin = seedUser(100L, Role.ADMIN, "CARD-ADMIN", "admin@fablab.io", "admin", "Direção");
    }

    @Test
    void logoutInvalidatesAccessTokenForProtectedEndpoints() throws Exception {
        String token = accessToken(admin, Role.ADMIN);

        mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout realizado com sucesso"));

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void blacklistedTokenCannotBeUsedAgainEvenForLogout() throws Exception {
        String token = accessToken(admin, Role.ADMIN);

        mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotatesTokensAndInvalidatesOldRefreshToken() throws Exception {
        String refreshToken = refreshToken(admin, Role.ADMIN);

        mockMvc.perform(post("/auth/refresh")
                        .contentType("application/json")
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType("application/json")
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRejectsAccessToken() throws Exception {
        String accessToken = accessToken(admin, Role.ADMIN);

        mockMvc.perform(post("/auth/refresh")
                        .contentType("application/json")
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(accessToken)))
                .andExpect(status().isUnauthorized());
    }
}