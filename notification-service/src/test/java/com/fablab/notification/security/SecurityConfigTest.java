package com.fablab.notification.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.notification.entity.NivelAcesso;
import com.fablab.notification.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

/** Verifica RBAC e respostas de autenticação/autorização. */
class SecurityConfigTest extends BaseIntegrationTest {

    @Test
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(get("/notificacoes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void tokenInvalidoRetorna401() throws Exception {
        mockMvc.perform(get("/notificacoes").header("Authorization", "Bearer nao-e-um-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void roleInvalidaRetorna401() throws Exception {
        mockMvc.perform(get("/notificacoes").header("Authorization", bearerComRole(1L, "INVALIDO")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioAutenticadoAcessaPropriasNotificacoes() throws Exception {
        mockMvc.perform(get("/notificacoes").header("Authorization", bearer(1L, NivelAcesso.BOLSISTA)))
                .andExpect(status().isOk());
    }

    @Test
    void usuarioComumNaoAcessaEndpointsAdmin() throws Exception {
        mockMvc.perform(get("/notificacoes/admin").header("Authorization", bearer(1L, NivelAcesso.BOLSISTA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void usuarioComumNaoAcessaConfiguracoesDeCanal() throws Exception {
        mockMvc.perform(get("/configuracoes-canal").header("Authorization", bearer(1L, NivelAcesso.BOLSISTA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminAcessaEndpointsAdmin() throws Exception {
        mockMvc.perform(get("/notificacoes/admin").header("Authorization", bearer(1L, NivelAcesso.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void healthNaoExigeToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
