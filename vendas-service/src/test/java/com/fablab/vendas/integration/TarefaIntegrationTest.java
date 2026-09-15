package com.fablab.vendas.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.vendas.entity.NivelAcesso;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class TarefaIntegrationTest extends BaseIntegrationTest {

    @Test
    void criaTarefaMarketing() throws Exception {
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(post("/tarefas-marketing")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Criar post Instagram","descricao":"Post sobre evento",
                                 "idResponsavel":10,"prioridade":"ALTA"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Criar post Instagram"))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    void listaTarefasPorResponsavel() throws Exception {
        String adminToken = token(1L, NivelAcesso.ADMIN);
        mockMvc.perform(post("/tarefas-marketing")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Tarefa A","idResponsavel":10,"prioridade":"MEDIA"}
                                """))
                .andExpect(status().isCreated());

        String token = token(2L, NivelAcesso.ESTAGIARIO);
        mockMvc.perform(get("/tarefas-marketing").param("idResponsavel", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Tarefa A"));
    }

    @Test
    void atualizaStatusTarefa() throws Exception {
        String adminToken = token(1L, NivelAcesso.ADMIN);
        var result = mockMvc.perform(post("/tarefas-marketing")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Tarefa Status","idResponsavel":20,"prioridade":"BAIXA"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        Long id = ((Number) com.jayway.jsonpath.JsonPath.read(body, "$.id")).longValue();

        mockMvc.perform(put("/tarefas-marketing/{id}/status", id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"EM_ANDAMENTO\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
    }

    @Test
    void atualizaTarefaCompleta() throws Exception {
        String adminToken = token(1L, NivelAcesso.ADMIN);
        var result = mockMvc.perform(post("/tarefas-marketing")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Original","idResponsavel":10,"prioridade":"BAIXA"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        Long id = ((Number) com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(put("/tarefas-marketing/{id}", id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Atualizado","idResponsavel":20,"prioridade":"ALTA"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Atualizado"));
    }

    @Test
    void listaTarefasPorStatus() throws Exception {
        String adminToken = token(1L, NivelAcesso.ADMIN);
        mockMvc.perform(post("/tarefas-marketing")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Tarefa P","idResponsavel":10,"prioridade":"MEDIA"}
                                """))
                .andExpect(status().isCreated());

        String token = token(2L, NivelAcesso.ESTAGIARIO);
        mockMvc.perform(get("/tarefas-marketing").param("status", "PENDENTE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDENTE"));
    }
}