package com.fablab.vendas.security;

import com.fablab.vendas.TokenHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Segurança/RBAC: endpoints protegidos sem token → 401; Recrutando sem
 * acesso; edição negada a não-criador (403, D-3).
 */
@SpringBootTest
@AutoConfigureMockMvc
class VendasSegurancaTest {

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
        mockMvc.perform(get("/clientes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/encomendas")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/tags")).andExpect(status().isUnauthorized());
    }

    @Test
    void recrutandoSemAcesso() throws Exception {
        mockMvc.perform(post("/clientes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(50L, "RECRUTANDO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoPessoa":"PF","nomeRazaoSocial":"X","cpfCnpj":"529.982.247-25"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void edicaoNegadaParaNaoCriador() throws Exception {
        String corpo = """
                {"tipoPessoa":"PF","nomeRazaoSocial":"João Silva","cpfCnpj":"529.982.247-25",
                 "email":"joao@email.com"}""";
        String resposta = mockMvc.perform(post("/clientes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(10L, "BOLSISTA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = com.jayway.jsonpath.JsonPath.parse(resposta).read("$.id", Number.class).longValue();

        mockMvc.perform(put("/clientes/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(99L, "BOLSISTA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/clientes/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk());
    }

    @Test
    void estagiarioLeMasNaoEdita() throws Exception {
        mockMvc.perform(get("/clientes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(20L, "ESTAGIARIO")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/clientes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(20L, "ESTAGIARIO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoPessoa":"PF","nomeRazaoSocial":"Y","cpfCnpj":"529.982.247-25"}"""))
                .andExpect(status().isForbidden());
    }
}
