package com.fablab.vendas.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.vendas.config.RabbitMqConfig;
import com.fablab.vendas.entity.Encomenda;
import com.fablab.vendas.entity.HistoricoStatusEncomenda;
import com.fablab.vendas.entity.NivelAcesso;
import com.fablab.vendas.entity.Orcamento;
import com.fablab.vendas.entity.StatusKanban;
import com.fablab.vendas.entity.StatusOrcamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class EncomendaIntegrationTest extends BaseIntegrationTest {

    private Long criarEncomenda(Long idCliente) throws Exception {
        String adminToken = token(1L, NivelAcesso.ADMIN);
        var result = mockMvc.perform(post("/encomendas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idCliente":%d,"valorFinal":100}
                                """.formatted(idCliente)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusKanban").value("FILA"))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        return ((Number) com.jayway.jsonpath.JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void criaEncomendaComHistorico() throws Exception {
        var cliente = seedCliente("Carlos");
        Long idEncomenda = criarEncomenda(cliente.getId());
        assertEquals(1, historicoRepository.findAll().size());
    }

    @Test
    void movimentaKanbanValido() throws Exception {
        var cliente = seedCliente("Carlos");
        Long idEncomenda = criarEncomenda(cliente.getId());
        String token = token(2L, NivelAcesso.BOLSISTA);

        mockMvc.perform(put("/encomendas/{id}/kanban", idEncomenda)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novoStatus\":\"PRODUCAO\",\"idUsuario\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusKanban").value("PRODUCAO"));

        mockMvc.perform(put("/encomendas/{id}/kanban", idEncomenda)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novoStatus\":\"ACABAMENTO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusKanban").value("ACABAMENTO"));

        mockMvc.perform(put("/encomendas/{id}/kanban", idEncomenda)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novoStatus\":\"PRONTO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusKanban").value("PRONTO"));

        mockMvc.perform(put("/encomendas/{id}/kanban", idEncomenda)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novoStatus\":\"ENTREGUE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusKanban").value("ENTREGUE"));

        assertEquals(5, historicoRepository.findAll().size());
    }

    @Test
    void movimentoInvalidoDaFilaParaAcabamento() throws Exception {
        var cliente = seedCliente("Carlos");
        Long idEncomenda = criarEncomenda(cliente.getId());
        String token = token(2L, NivelAcesso.BOLSISTA);

        mockMvc.perform(put("/encomendas/{id}/kanban", idEncomenda)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novoStatus\":\"ACABAMENTO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void movimentaProducaoParaEntregueDireto() throws Exception {
        var cliente = seedCliente("Carlos");
        Long idEncomenda = criarEncomenda(cliente.getId());
        String token = token(2L, NivelAcesso.BOLSISTA);

        // FILA → PRODUCAO
        mockMvc.perform(put("/encomendas/{id}/kanban", idEncomenda)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novoStatus\":\"PRODUCAO\"}"))
                .andExpect(status().isOk());

        // PRODUCAO → ENTREGUE (direto)
        mockMvc.perform(put("/encomendas/{id}/kanban", idEncomenda)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novoStatus\":\"ENTREGUE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusKanban").value("ENTREGUE"));

        verify(rabbitTemplate, times(1)).convertAndSend(
                org.mockito.ArgumentMatchers.eq(RabbitMqConfig.VENDAS_EXCHANGE),
                org.mockito.ArgumentMatchers.eq(RabbitMqConfig.ENCOMENDA_ENTREGUE_ROUTING_KEY),
                org.mockito.ArgumentMatchers.any(Object.class));
    }

    @Test
    void entregueENaoMoveMais() throws Exception {
        var cliente = seedCliente("Carlos");
        Long idEncomenda = criarEncomenda(cliente.getId());
        String token = token(2L, NivelAcesso.BOLSISTA);

        mockMvc.perform(put("/encomendas/{id}/kanban", idEncomenda)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novoStatus\":\"PRODUCAO\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/encomendas/{id}/kanban", idEncomenda)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novoStatus\":\"ENTREGUE\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/encomendas/{id}/kanban", idEncomenda)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novoStatus\":\"PRONTO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listaEncomendasPorStatus() throws Exception {
        var cliente = seedCliente("Carlos");
        criarEncomenda(cliente.getId());
        String token = token(2L, NivelAcesso.ESTAGIARIO);

        mockMvc.perform(get("/encomendas").param("status", "FILA")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].statusKanban").value("FILA"));
    }

    @Test
    void buscaEncomendaPorId() throws Exception {
        var cliente = seedCliente("Carlos");
        Long idEncomenda = criarEncomenda(cliente.getId());
        String token = token(2L, NivelAcesso.ESTAGIARIO);

        mockMvc.perform(get("/encomendas/{id}", idEncomenda)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idEncomenda.intValue()));
    }

    @Test
    void historicoVazioParaEncomendaNova() throws Exception {
        var cliente = seedCliente("Carlos");
        Long idEncomenda = criarEncomenda(cliente.getId());
        String token = token(2L, NivelAcesso.ESTAGIARIO);

        mockMvc.perform(get("/encomendas/{id}/historico", idEncomenda)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].statusNovo").value("FILA"));
    }
}