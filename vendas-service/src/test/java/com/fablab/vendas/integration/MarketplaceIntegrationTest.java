package com.fablab.vendas.integration;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.vendas.config.RabbitMqConfig;
import com.fablab.vendas.entity.Encomenda;
import com.fablab.vendas.entity.NivelAcesso;
import com.fablab.vendas.entity.StatusKanban;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class MarketplaceIntegrationTest extends BaseIntegrationTest {

    private Long criarEncomendaSemOrcamento(Long idCliente) throws Exception {
        String adminToken = token(1L, NivelAcesso.ADMIN);
        var result = mockMvc.perform(post("/encomendas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idCliente":%d,"valorFinal":50}
                                """.formatted(idCliente)))
                .andExpect(status().isCreated())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        return ((Number) com.jayway.jsonpath.JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void registraVendaMarketplace() throws Exception {
        var cliente = seedCliente("Loja X");
        Long idEncomenda = criarEncomendaSemOrcamento(cliente.getId());
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(post("/marketplace")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idEncomenda":%d,"plataforma":"Mercado Livre",
                                 "codigoExterno":"ML-12345","valorTaxa":10.50}
                                """.formatted(idEncomenda)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plataforma").value("Mercado Livre"))
                .andExpect(jsonPath("$.valorTaxa").value(10.5));

        verify(rabbitTemplate, times(1)).convertAndSend(
                org.mockito.ArgumentMatchers.eq(RabbitMqConfig.VENDAS_EXCHANGE),
                org.mockito.ArgumentMatchers.eq(RabbitMqConfig.MARKETPLACE_VENDA_ROUTING_KEY),
                org.mockito.ArgumentMatchers.any(Object.class));
    }

    @Test
    void registraVendaSemCodigoExterno() throws Exception {
        var cliente = seedCliente("Loja Y");
        Long idEncomenda = criarEncomendaSemOrcamento(cliente.getId());
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(post("/marketplace")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idEncomenda":%d,"plataforma":"Shopee"}
                                """.formatted(idEncomenda)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dataVenda").exists());
    }

    @Test
    void registraVendaEncomendaInexistente() throws Exception {
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(post("/marketplace")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idEncomenda":999999,"plataforma":"ML"}
                                """))
                .andExpect(status().isNotFound());
    }
}