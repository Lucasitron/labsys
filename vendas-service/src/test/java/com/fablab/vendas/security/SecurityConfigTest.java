package com.fablab.vendas.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.vendas.entity.NivelAcesso;
import com.fablab.vendas.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SecurityConfigTest extends BaseIntegrationTest {

    @Test
    void requisicaoSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/clientes")).andExpect(status().isUnauthorized());
    }

    @Test
    void estagiarioPodeVisualizarClientes() throws Exception {
        String token = token(1L, NivelAcesso.ESTAGIARIO);
        mockMvc.perform(get("/clientes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void estagiarioNaoPodeCriarCliente() throws Exception {
        String token = token(1L, NivelAcesso.ESTAGIARIO);
        mockMvc.perform(post("/clientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoPessoa":"PF","nomeRazaoSocial":"Teste","cpfCnpj":"52998224725"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void recrutandoNaoPodeAcessarClientes() throws Exception {
        String token = token(1L, NivelAcesso.RECRUTANDO);
        mockMvc.perform(get("/clientes").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void bolsistaPodeCriarCliente() throws Exception {
        String token = token(1L, NivelAcesso.BOLSISTA);
        mockMvc.perform(post("/clientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoPessoa":"PF","nomeRazaoSocial":"Bolsista","cpfCnpj":"52998224725"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void adminPodeCriarTag() throws Exception {
        String token = token(1L, NivelAcesso.ADMIN);
        mockMvc.perform(post("/tags")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Laser","cor":"#ff0000"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void voluntarioNaoPodeAcessarTarefas() throws Exception {
        String token = token(1L, NivelAcesso.VOLUNTARIO);
        mockMvc.perform(get("/tarefas-marketing").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}