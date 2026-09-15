package com.fablab.vendas.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.vendas.config.RabbitMqConfig;
import com.fablab.vendas.entity.NivelAcesso;
import com.fablab.vendas.entity.StatusKanban;
import com.fablab.vendas.entity.StatusOrcamento;
import com.fablab.vendas.entity.TipoPessoa;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class OrcamentoIntegrationTest extends BaseIntegrationTest {

    @Test
    void criaOrcamentoComItensECalculaTotal() throws Exception {
        var cliente = seedCliente("Maria");
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(post("/orcamentos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idCliente":%d,"validade":"2026-10-01",
                                 "itens":[{"descricao":"Peça A","quantidade":2,"valorUnitario":10},
                                          {"descricao":"Peça B","quantidade":1,"valorUnitario":5}]}
                                """.formatted(cliente.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorTotal").value(25.0))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.itens.length()").value(2));
    }

    @Test
    void atualizaOrcamentoPendente() throws Exception {
        var cliente = seedCliente("Maria");
        var orcamento = seedOrcamento(cliente.getId(), StatusOrcamento.PENDENTE, 1);
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(put("/orcamentos/{id}", orcamento.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idCliente":%d,"validade":"2026-11-01",
                                 "itens":[{"descricao":"Novo item","quantidade":3,"valorUnitario":7}]}
                                """.formatted(cliente.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorTotal").value(21.0));
    }

    @Test
    void naoAtualizaOrcamentoAprovado() throws Exception {
        var cliente = seedCliente("Maria");
        var orcamento = seedOrcamento(cliente.getId(), StatusOrcamento.APROVADO, 1);
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(put("/orcamentos/{id}", orcamento.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idCliente":%d,"validade":"2026-11-01",
                                 "itens":[{"descricao":"Item","quantidade":1,"valorUnitario":9}]}
                                """.formatted(cliente.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aprovaOrcamentoEPublicaEvento() throws Exception {
        var cliente = seedCliente("Maria");
        var orcamento = seedOrcamento(cliente.getId(), StatusOrcamento.PENDENTE, 1);
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(put("/orcamentos/{id}/status", orcamento.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APROVADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADO"));

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMqConfig.VENDAS_EXCHANGE),
                eq(RabbitMqConfig.ORCAMENTO_APROVADO_ROUTING_KEY), any(Object.class));
    }

    @Test
    void converteOrcamentoAprovadoEmEncomenda() throws Exception {
        var cliente = seedCliente("Maria");
        var orcamento = seedOrcamento(cliente.getId(), StatusOrcamento.APROVADO, 2);
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(post("/orcamentos/{id}/encomenda", orcamento.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idCliente":%d,"dataPrevisaoEntrega":"2026-10-10"}
                                """.formatted(cliente.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusKanban").value("FILA"))
                .andExpect(jsonPath("$.idOrcamento").value(orcamento.getId().intValue()))
                .andExpect(jsonPath("$.valorFinal").value(20.0));

        assertEquals(1, historicoRepository.findAll().size());
        assertEquals(StatusKanban.FILA, historicoRepository.findAll().get(0).getStatusNovo());
    }

    @Test
    void naoConverteOrcamentoPendente() throws Exception {
        var cliente = seedCliente("Maria");
        var orcamento = seedOrcamento(cliente.getId(), StatusOrcamento.PENDENTE, 1);
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(post("/orcamentos/{id}/encomenda", orcamento.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idCliente":%d}
                                """.formatted(cliente.getId())))
                .andExpect(status().isBadRequest());

        assertTrue(encomendaRepository.findAll().isEmpty());
    }
}