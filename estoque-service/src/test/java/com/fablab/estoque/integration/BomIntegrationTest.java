package com.fablab.estoque.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.estoque.entity.Categoria;
import com.fablab.estoque.entity.ListaMateriais;
import com.fablab.estoque.entity.NivelAcesso;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class BomIntegrationTest extends BaseIntegrationTest {

    @Test
    void adminCriaBom() throws Exception {
        var item = seedItem("Resistor", Categoria.INSUMO, BigDecimal.TEN, BigDecimal.ONE);
        String adminToken = token(1L, NivelAcesso.ADMIN);

        mockMvc.perform(post("/boms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idProdutoServico":10,"nome":"BOM Teste","versao":1,"editavel":true,
                                 "itens":[{"idItem":%d,"quantidadePrevista":5}]}
                                """.formatted(item.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itens[0].quantidadePrevista").value(5));
    }

    @Test
    void buscaBom() throws Exception {
        var item = seedItem("Parafuso", Categoria.PECA, BigDecimal.valueOf(100), BigDecimal.TEN);
        var bom = new ListaMateriais();
        bom.setIdProdutoServico(1L);
        bom.setNome("BOM Parafusos");
        bom.setVersao(1);
        bom.setEditavel(true);
        var ib = new com.fablab.estoque.entity.ItemBom();
        ib.setItem(item);
        ib.setQuantidadePrevista(BigDecimal.TEN);
        bom.adicionarItem(ib);
        bom = bomRepository.save(bom);

        String adminToken = token(1L, NivelAcesso.ADMIN);
        mockMvc.perform(get("/boms/{id}", bom.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].quantidadePrevista").value(10));
    }

    @Test
    void atualizaBomIncrementaVersao() throws Exception {
        var item = seedItem("Arruela", Categoria.PECA, BigDecimal.TEN, BigDecimal.ONE);
        var bom = new ListaMateriais();
        bom.setIdProdutoServico(1L);
        bom.setNome("BOM");
        bom.setVersao(1);
        bom.setEditavel(true);
        var ib = new com.fablab.estoque.entity.ItemBom();
        ib.setItem(item);
        ib.setQuantidadePrevista(BigDecimal.TEN);
        bom.adicionarItem(ib);
        bom = bomRepository.save(bom);

        String adminToken = token(1L, NivelAcesso.ADMIN);
        mockMvc.perform(put("/boms/{id}", bom.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idProdutoServico":1,"nome":"BOM v2","editavel":false,
                                 "itens":[{"idItem":%d,"quantidadePrevista":20}]}
                                """.formatted(item.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versao").value(2))
                .andExpect(jsonPath("$.editavel").value(false));
    }

    @Test
    void adminRegistraConsumoNaBom() throws Exception {
        var item = seedItem("Filamento", Categoria.INSUMO, BigDecimal.valueOf(100), BigDecimal.TEN);
        var bom = new ListaMateriais();
        bom.setIdProdutoServico(1L);
        bom.setNome("BOM Filamento");
        bom.setVersao(1);
        bom.setEditavel(true);
        var ib = new com.fablab.estoque.entity.ItemBom();
        ib.setItem(item);
        ib.setQuantidadePrevista(BigDecimal.valueOf(50));
        bom.adicionarItem(ib);
        bom = bomRepository.save(bom);

        String adminToken = token(1L, NivelAcesso.ADMIN);
        mockMvc.perform(post("/boms/{id}/consumo", bom.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens":[{"idItem":%d,"quantidadeConsumida":30}]}
                                """.formatted(item.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].quantidadeReal").value(30));
    }

    @Test
    void naoAdminNaoPodeCriarBom() throws Exception {
        var item = seedItem("Resistor", Categoria.INSUMO, BigDecimal.TEN, BigDecimal.ONE);
        String token = token(1L, NivelAcesso.VOLUNTARIO);
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
    void consumaItemNaoNaBomRetorna400() throws Exception {
        var item1 = seedItem("Resistor", Categoria.INSUMO, BigDecimal.TEN, BigDecimal.ONE);
        var item2 = seedItem("Capacitor", Categoria.PECA, BigDecimal.TEN, BigDecimal.ONE);
        var bom = new ListaMateriais();
        bom.setIdProdutoServico(1L);
        bom.setNome("BOM Resistor");
        bom.setVersao(1);
        bom.setEditavel(true);
        var ib = new com.fablab.estoque.entity.ItemBom();
        ib.setItem(item1);
        ib.setQuantidadePrevista(BigDecimal.TEN);
        bom.adicionarItem(ib);
        bom = bomRepository.save(bom);

        String adminToken = token(1L, NivelAcesso.ADMIN);
        mockMvc.perform(post("/boms/{id}/consumo", bom.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens":[{"idItem":%d,"quantidadeConsumida":5}]}
                                """.formatted(item2.getId())))
                .andExpect(status().isBadRequest());
    }
}