package com.fablab.auth.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.auth.entity.Login;
import com.fablab.auth.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthControllerIntegrationTest extends BaseIntegrationTest {

    private Login admin;
    private Login bolsista;

    @BeforeEach
    void seed() {
        admin = seedUser(100L, Role.ADMIN, "CARD-ADMIN", "admin@fablab.io", "admin", "Direção");
        bolsista = seedUser(101L, Role.BOLSISTA, "CARD-BOLSISTA", "bolsista@fablab.io", "bolsista", "Laboratório");
    }

    @Test
    void loginByEmailReturnsTokenPair() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"admin@fablab.io","senha":"%s"}
                                """.formatted(SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.idUser").value(100))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.setor").value("Direção"));
    }

    @Test
    void loginByNomeUsuarioReturnsTokenPair() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"nomeUsuario":"bolsista","senha":"%s"}
                                """.formatted(SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("BOLSISTA"));
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"admin@fablab.io","senha":"SenhaErrada"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void loginWithUnknownUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"ghost@fablab.io","senha":"Senha@123"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithoutPasswordReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"admin@fablab.io"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.senha").value("senha é obrigatória"));
    }

    @Test
    void meReturnsAuthenticatedUserDetails() throws Exception {
        String token = accessToken(admin, Role.ADMIN);

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUser").value(100))
                .andExpect(jsonPath("$.email").value("admin@fablab.io"))
                .andExpect(jsonPath("$.permissions[0].role").value("ADMIN"));
    }

    @Test
    void meWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void meWithBolsistaTokenReturnsOwnDetails() throws Exception {
        String token = accessToken(bolsista, Role.BOLSISTA);

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUser").value(101))
                .andExpect(jsonPath("$.permissions[0].role").value("BOLSISTA"));
    }
}