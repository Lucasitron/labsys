package com.fablab.estoque.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.estoque.TokenHelper;
import com.fablab.estoque.entity.Categoria;
import com.fablab.estoque.entity.Fornecedor;
import com.fablab.estoque.entity.NivelAcesso;
import com.fablab.estoque.integration.BaseIntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SecurityConfigTest extends BaseIntegrationTest {

    @Test
    void requisicaoSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/itens")).andExpect(status().isUnauthorized());
    }

    @Test
    void bolsistaPodeCriarItem() throws Exception {
        String token = token(1L, NivelAcesso.BOLSISTA);
        mockMvc.perform(post("/itens")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"X","categoria":"INSUMO","unidadeMedida":"un",
                                 "quantidadeAtual":1,"estoqueMinimo":1}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void bolsistaPodeRegistrarEntrada() throws Exception {
        String token = token(1L, NivelAcesso.BOLSISTA);
        var item = seedItem("Fita", Categoria.INSUMO, BigDecimal.TEN, BigDecimal.ONE);
        var forn = new Fornecedor();
        forn.setNome("Sup");
        forn.setCnpj("99");
        forn = fornecedorRepository.save(forn);
        mockMvc.perform(post("/entradas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idItem":%d,"idFornecedor":%d,"quantidade":5,"valorUnitario":1}
                                """.formatted(item.getId(), forn.getId())))
                .andExpect(status().isCreated());
    }

    @Test
    void estagiarioNaoPodeCriarBom() throws Exception {
        String token = token(1L, NivelAcesso.ESTAGIARIO);
        var item = seedItem("Resistor", Categoria.INSUMO, BigDecimal.TEN, BigDecimal.ONE);
        mockMvc.perform(post("/boms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idProdutoServico":1,"nome":"X",
                                 "itens":[{"idItem":%d,"quantidadePrevista":1}]}
                                """.formatted(item.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void recrutandoNaoPodeAcessarItens() throws Exception {
        String token = token(1L, NivelAcesso.RECRUTANDO);
        mockMvc.perform(get("/itens").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPodeCriarFornecedor() throws Exception {
        String adminToken = token(1L, NivelAcesso.ADMIN);
        mockMvc.perform(post("/fornecedores")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Digikey","cnpj":"123456000199"}
                                """))
                .andExpect(status().isCreated());
    }
}