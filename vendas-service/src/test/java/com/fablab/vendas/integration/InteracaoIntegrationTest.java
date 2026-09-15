package com.fablab.vendas.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.vendas.entity.NivelAcesso;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class InteracaoIntegrationTest extends BaseIntegrationTest {

    @Test
    void registraInteracaoComCliente() throws Exception {
        var cliente = seedCliente("Roberto");
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(post("/interacoes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idCliente":%d,"tipo":"TELEFONE",
                                 "descricao":"Ligação sobre orçamento","idUsuario":10}
                                """.formatted(cliente.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("TELEFONE"));
    }

    @Test
    void listaInteracoesPorCliente() throws Exception {
        var cliente = seedCliente("Roberto");
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(post("/interacoes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idCliente":%d,"tipo":"EMAIL","descricao":"Envio proposta"}
                                """.formatted(cliente.getId())))
                .andExpect(status().isCreated());

        String token = token(2L, NivelAcesso.ESTAGIARIO);
        mockMvc.perform(get("/interacoes/{idCliente}", cliente.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("EMAIL"));
    }

    @Test
    void listaInteracoesClienteInexistente() throws Exception {
        String token = token(2L, NivelAcesso.ESTAGIARIO);

        mockMvc.perform(get("/interacoes/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}