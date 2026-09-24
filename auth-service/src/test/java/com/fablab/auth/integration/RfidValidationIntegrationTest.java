package com.fablab.auth.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.auth.dto.RfidAccessEvent;
import com.fablab.auth.entity.Login;
import com.fablab.auth.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RfidValidationIntegrationTest extends BaseIntegrationTest {

    private Login admin;

    @BeforeEach
    void seed() {
        admin = seedUser(100L, Role.ADMIN, "CARD-ADMIN", "admin@fablab.io", "admin", "Direção");
    }

    @Test
    void validateKnownRfidRegistersEntradaAndPublishesEvent() throws Exception {
        mockMvc.perform(post("/auth/validate-rfid")
                        .contentType("application/json")
                        .content("""
                                {"uuid":"CARD-ADMIN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.idUser").value(100))
                .andExpect(jsonPath("$.uuidRfid").value("CARD-ADMIN"))
                .andExpect(jsonPath("$.nomeUsuario").value("admin"))
                .andExpect(jsonPath("$.type").value("ENTRADA"));

        verify(rabbitTemplate).convertAndSend(
                eq("fablab.access"),
                eq("access.rfid.event"),
                any(RfidAccessEvent.class));
    }

    @Test
    void validateKnownRfidTogglesToSaidaOnSecondScan() throws Exception {
        mockMvc.perform(post("/auth/validate-rfid")
                        .contentType("application/json")
                        .content("""
                                {"uuid":"CARD-ADMIN"}
                                """))
                .andExpect(jsonPath("$.type").value("ENTRADA"));

        mockMvc.perform(post("/auth/validate-rfid")
                        .contentType("application/json")
                        .content("""
                                {"uuid":"CARD-ADMIN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("SAIDA"));
    }

    @Test
    void validateUnknownRfidLogsDeniedWithoutEvent() throws Exception {
        mockMvc.perform(post("/auth/validate-rfid")
                        .contentType("application/json")
                        .content("""
                                {"uuid":"CARD-DESCONHECIDO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(false))
                .andExpect(jsonPath("$.type").value("ACESSO_NEGADO"))
                .andExpect(jsonPath("$.idUser").doesNotExist());

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void validateRfidWithoutUuidReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/auth/validate-rfid")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.uuid").value("uuid é obrigatório"));
    }

    @Test
    void rfidEndpointIsPublic() throws Exception {
        mockMvc.perform(post("/auth/validate-rfid")
                        .contentType("application/json")
                        .content("""
                                {"uuid":"CARD-ADMIN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true));
    }
}