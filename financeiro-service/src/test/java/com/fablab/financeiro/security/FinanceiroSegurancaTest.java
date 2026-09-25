package com.fablab.financeiro.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.financeiro.TokenHelper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Segurança/RBAC Admin-only (D-3): sem token → 401; níveis não-Admin → 403
 * em todos os grupos de endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FinanceiroSegurancaTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TokenHelper tokenHelper;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    private String bearer(Long id, String role) {
        return "Bearer " + tokenHelper.token(id, role);
    }

    @Test
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(get("/lancamentos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/categorias")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/doacoes-recursos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/valores-hora")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/relatorios/fluxo-caixa")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/custos-encomenda/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void naoAdminRecebe403() throws Exception {
        for (String role : new String[]{"BOLSISTA", "VOLUNTARIO", "ESTAGIARIO", "RECRUTANDO"}) {
            mockMvc.perform(get("/lancamentos")
                            .header(HttpHeaders.AUTHORIZATION, bearer(10L, role)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/relatorios/dre")
                            .header(HttpHeaders.AUTHORIZATION, bearer(10L, role)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/categorias")
                            .header(HttpHeaders.AUTHORIZATION, bearer(10L, role))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nome\":\"X\",\"tipo\":\"RECEITA\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void adminAcessa() throws Exception {
        mockMvc.perform(get("/lancamentos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1L, "ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/relatorios/inadimplencia")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1L, "ADMIN")))
                .andExpect(status().isOk());
    }
}
