package com.fablab.estoque.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.estoque.TokenHelper;
import com.fablab.estoque.entity.Categoria;
import com.fablab.estoque.entity.Fornecedor;
import com.fablab.estoque.entity.Localizacao;
import com.fablab.estoque.entity.NivelAcesso;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ItemIntegrationTest extends BaseIntegrationTest {

    @Test
    void adminCriaItemEPublicaEstoqueBaixo() throws Exception {
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(post("/itens")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Resistor 10k","categoria":"INSUMO",
                                 "unidadeMedida":"un","quantidadeAtual":5,"estoqueMinimo":10}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void estagiarioListaItens() throws Exception {
        seedItem("Fita", Categoria.INSUMO, BigDecimal.TEN, BigDecimal.ONE);
        String token = token(2L, NivelAcesso.ESTAGIARIO);

        mockMvc.perform(get("/itens").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Fita"));
    }

    @Test
    void recrutandoNaoPodeAcessarItens() throws Exception {
        String token = token(1L, NivelAcesso.RECRUTANDO);
        mockMvc.perform(get("/itens").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void naoOperacionalNaoPodeCriarItem() throws Exception {
        String token = token(1L, NivelAcesso.ESTAGIARIO);
        mockMvc.perform(post("/itens")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"X","categoria":"INSUMO","unidadeMedida":"un",
                                 "quantidadeAtual":1,"estoqueMinimo":1}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRegistraEntradaAumentaEstoque() throws Exception {
        var item = seedItem("Fita", Categoria.INSUMO, BigDecimal.TEN, BigDecimal.ONE);
        var forn = new Fornecedor();
        forn.setNome("Digikey");
        forn.setCnpj("12.345/0001-99");
        forn = fornecedorRepository.save(forn);
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(post("/entradas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idItem":%d,"idFornecedor":%d,"quantidade":20,"valorUnitario":0.5}
                                """.formatted(item.getId(), forn.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorTotal").value(10.0));
    }

    @Test
    void adminRegistraSaidaInsuficiente409() throws Exception {
        var item = seedItem("Resistor", Categoria.INSUMO, BigDecimal.valueOf(5), BigDecimal.ONE);
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(post("/saidas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idItem":%d,"quantidade":20,"tipoSaida":"CONSUMO"}
                                """.formatted(item.getId())))
                .andExpect(status().isConflict());
    }

    @Test
    void buscaItemPorId() throws Exception {
        var item = seedItem("Capacitor", Categoria.PECA, BigDecimal.TEN, BigDecimal.ONE);
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(get("/itens/{id}", item.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Capacitor"));
    }

    @Test
    void adminAtualizaItem() throws Exception {
        var item = seedItem("Fita", Categoria.INSUMO, BigDecimal.TEN, BigDecimal.ONE);
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(put("/itens/{id}", item.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Fita Adesiva","categoria":"INSUMO","unidadeMedida":"m",
                                 "quantidadeAtual":10,"estoqueMinimo":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Fita Adesiva"));
    }

    @Test
    void listarLocalizacoes() throws Exception {
        seedLocalizacao("A1", "P2", "C3");
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(get("/localizacoes").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].armario").value("A1"));
    }

    @Test
    void listarFornecedores() throws Exception {
        var forn = new Fornecedor();
        forn.setNome("Supplier");
        forn.setCnpj("99");
        fornecedorRepository.save(forn);
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(get("/fornecedores").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Supplier"));
    }

    @Test
    void bolsistaPodeCriarItem() throws Exception {
        String token = token(1L, NivelAcesso.BOLSISTA);
        mockMvc.perform(post("/itens")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Parafuso","categoria":"PECA","unidadeMedida":"un",
                                 "quantidadeAtual":10,"estoqueMinimo":2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Parafuso"));
    }

    @Test
    void adminCriaLocalizacao() throws Exception {
        String adminToken = token(1L, NivelAcesso.ADMIN);
        mockMvc.perform(post("/localizacoes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"armario":"A1","prateleira":"P2","caixa":"C3","descricao":"Cx elétrica"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.armario").value("A1"));
    }

    @Test
    void detalheDeEntrada() throws Exception {
        var item = seedItem("Fita", Categoria.INSUMO, BigDecimal.TEN, BigDecimal.ONE);
        var forn = new Fornecedor();
        forn.setNome("Digikey");
        forn.setCnpj("123456000199");
        forn = fornecedorRepository.save(forn);
        String adminToken = token(1L, NivelAcesso.ADMIN);

        String response = mockMvc.perform(post("/entradas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idItem":%d,"idFornecedor":%d,"quantidade":10,"valorUnitario":1}
                                """.formatted(item.getId(), forn.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long entradaId = com.fasterxml.jackson.databind.ObjectMapper.class.getDeclaredConstructor().newInstance()
                .readTree(response).get("id").asLong();

        mockMvc.perform(get("/entradas/{id}", entradaId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(10));
    }

    @Test
    void detalheDeSaida() throws Exception {
        var item = seedItem("Resistor", Categoria.INSUMO, BigDecimal.TEN, BigDecimal.ONE);
        String adminToken = token(1L, NivelAcesso.ADMIN);

        String response = mockMvc.perform(post("/saidas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idItem":%d,"quantidade":2,"tipoSaida":"CONSUMO"}
                                """.formatted(item.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long saidaId = com.fasterxml.jackson.databind.ObjectMapper.class.getDeclaredConstructor().newInstance()
                .readTree(response).get("id").asLong();

        mockMvc.perform(get("/saidas/{id}", saidaId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoSaida").value("CONSUMO"));
    }

    @Test
    void exportarItensCsv() throws Exception {
        seedItem("Fita", Categoria.INSUMO, BigDecimal.TEN, BigDecimal.ONE);
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(get("/itens/export").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentTypeCompatibleWith("text/csv"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("id_item;nome")));
    }

    @Test
    void importarItensCsv() throws Exception {
        String adminToken = token(1L, NivelAcesso.ADMIN);
        String csv = "Placa MDF;Chapa 3mm;INSUMO;m2;10;2;\n";

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/itens/import")
                        .file(new org.springframework.mock.web.MockMultipartFile("arquivo", "itens.csv",
                                "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].nome").value("Placa MDF"));
    }
}