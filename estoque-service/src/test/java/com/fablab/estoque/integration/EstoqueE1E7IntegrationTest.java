package com.fablab.estoque.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.estoque.entity.Categoria;
import com.fablab.estoque.entity.Fornecedor;
import com.fablab.estoque.entity.NivelAcesso;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Cobertura dos gaps E-1..E-7 (§6.2): listas globais, BOM por projeto,
 * responsável, pessoa no empréstimo, RBAC de leitura e escape CSV.
 */
class EstoqueE1E7IntegrationTest extends BaseIntegrationTest {

    @Test
    void listaGlobalDeEntradasComResponsavel() throws Exception {
        var item = seedItem("Resistor", Categoria.INSUMO, BigDecimal.TEN, BigDecimal.ONE);
        var forn = new Fornecedor();
        forn.setNome("Digikey");
        forn = fornecedorRepository.save(forn);
        String token = token(1L, NivelAcesso.BOLSISTA);

        mockMvc.perform(post("/entradas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idItem":%d,"idFornecedor":%d,"quantidade":2,
                                 "valorUnitario":1.5,"responsavel":"Maria Silva"}
                                """.formatted(item.getId(), forn.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.responsavel").value("Maria Silva"));

        mockMvc.perform(get("/entradas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].responsavel").value("Maria Silva"));

        mockMvc.perform(get("/entradas").param("idItem", item.getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listaGlobalDeSaidasComResponsavel() throws Exception {
        var item = seedItem("Fio", Categoria.INSUMO, BigDecimal.TEN, BigDecimal.ONE);
        String token = token(1L, NivelAcesso.BOLSISTA);

        mockMvc.perform(post("/saidas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idItem":%d,"quantidade":1,"tipoSaida":"CONSUMO",
                                 "responsavel":"João Souza"}
                                """.formatted(item.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.responsavel").value("João Souza"));

        mockMvc.perform(get("/saidas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void emprestimosListaGlobalStatusPessoaERbacLeitura() throws Exception {
        var item = seedItem("Furadeira", Categoria.FERRAMENTA, BigDecimal.TEN, BigDecimal.ONE);
        String adminToken = token(1L, NivelAcesso.ADMIN);
        String donoToken = token(7L, NivelAcesso.VOLUNTARIO);
        String outroToken = token(8L, NivelAcesso.VOLUNTARIO);

        String response = mockMvc.perform(post("/emprestimos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idItem":%d,"idPessoa":7,"quantidade":1,
                                 "dataDevolucaoPrevista":"2026-12-31","responsavel":"Ana"}
                                """.formatted(item.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pessoa").value("Pessoa #7"))
                .andExpect(jsonPath("$.responsavel").value("Ana"))
                .andReturn().getResponse().getContentAsString();
        Long empId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response).get("id").asLong();

        mockMvc.perform(get("/emprestimos").param("status", "ativos")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/emprestimos").param("status", "historico")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/emprestimos").param("status", "invalido")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/emprestimos/{id}", empId)
                        .header("Authorization", "Bearer " + donoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pessoa").value("Pessoa #7"));

        mockMvc.perform(get("/emprestimos/{id}", empId)
                        .header("Authorization", "Bearer " + outroToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/emprestimos")
                        .header("Authorization", "Bearer " + outroToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(put("/emprestimos/{id}/devolucao", empId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/emprestimos").param("status", "historico")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void bomsPorProjetoId() throws Exception {
        var item = seedItem("Parafuso", Categoria.INSUMO, BigDecimal.TEN, BigDecimal.ONE);
        String token = token(1L, NivelAcesso.BOLSISTA);

        mockMvc.perform(post("/boms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idProdutoServico":42,"nome":"BOM X",
                                 "itens":[{"idItem":%d,"quantidadePrevista":4}]}
                                """.formatted(item.getId())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/boms").param("projetoId", "42")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/boms").param("projetoId", "99")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void exportCsvEscapaFormulas() throws Exception {
        var item = seedItem("=1+1", Categoria.INSUMO, BigDecimal.TEN, BigDecimal.ONE);
        String token = token(1L, NivelAcesso.BOLSISTA);

        String csv = mockMvc.perform(get("/itens/export")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertTrue(csv.contains("'=1+1"),
                "CSV deve neutralizar fórmula (CWE-1236): " + csv);
        org.junit.jupiter.api.Assertions.assertTrue(item.getId() != null);
    }
}
