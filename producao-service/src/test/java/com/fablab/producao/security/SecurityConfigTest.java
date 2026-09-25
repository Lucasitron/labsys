package com.fablab.producao.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.producao.entity.NivelAcesso;
import com.fablab.producao.integration.BaseIntegrationTest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SecurityConfigTest extends BaseIntegrationTest {

    @Test
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(get("/projetos")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoRetorna401() throws Exception {
        mockMvc.perform(get("/projetos").header("Authorization", "Bearer invalido.aqui"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminListaProjetos() throws Exception {
        mockMvc.perform(get("/projetos").header("Authorization", bearer(1L, NivelAcesso.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void estagiarioPodeVisualizar() throws Exception {
        mockMvc.perform(get("/projetos").header("Authorization", bearer(1L, NivelAcesso.ESTAGIARIO)))
                .andExpect(status().isOk());
    }

    @Test
    void recrutandoNaoTemAcesso() throws Exception {
        mockMvc.perform(get("/projetos").header("Authorization", bearer(1L, NivelAcesso.RECRUTANDO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void estagiarioNaoPodeCriarProjeto() throws Exception {
        mockMvc.perform(post("/projetos")
                        .header("Authorization", bearer(1L, NivelAcesso.ESTAGIARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"P","dataInicio":"%s","idResponsavel":1}
                                """.formatted(LocalDate.now())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCriaProjeto() throws Exception {
        mockMvc.perform(post("/projetos")
                        .header("Authorization", bearer(1L, NivelAcesso.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Braço Robótico","dataInicio":"%s","idResponsavel":1}
                                """.formatted(LocalDate.now())))
                .andExpect(status().isCreated());
    }

    @Test
    void bolsistaNaoPodeRegistrarInspecaoDeOutroInspetor() throws Exception {
        mockMvc.perform(post("/inspecoes-5s")
                        .header("Authorization", bearer(1L, NivelAcesso.BOLSISTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idSetor":1,"idInspetor":2,"dataInspecao":"%s","turno":"MANHA","itens":[]}
                                """.formatted(LocalDate.now())))
                .andExpect(status().isForbidden());
    }

    @Test
    void bolsistaNaoPodeRegistrarAdvertencia() throws Exception {
        mockMvc.perform(post("/advertencias")
                        .header("Authorization", bearer(1L, NivelAcesso.BOLSISTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idFuncionario":2,"motivo":"x","tipo":"VERBAL"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void bolsistaNaoPodeAlterarParametro() throws Exception {
        mockMvc.perform(put("/parametros-5s/1")
                        .header("Authorization", bearer(1L, NivelAcesso.BOLSISTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"valor":"30"}
                                """))
                .andExpect(status().isForbidden());
    }
}