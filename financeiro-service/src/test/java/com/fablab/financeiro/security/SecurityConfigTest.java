package com.fablab.financeiro.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.financeiro.entity.NivelAcesso;
import com.fablab.financeiro.integration.BaseIntegrationTest;
import com.fablab.financeiro.entity.TipoCategoriaFinanceira;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SecurityConfigTest extends BaseIntegrationTest {

    @Test
    void requisicaoSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/lancamentos")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminPodeListarLancamentos() throws Exception {
        String token = token(1L, NivelAcesso.ADMIN);
        mockMvc.perform(get("/lancamentos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void bolsistaNaoPodeAcessarLancamentos() throws Exception {
        String token = token(1L, NivelAcesso.BOLSISTA);
        mockMvc.perform(get("/lancamentos").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void voluntarioNaoPodeAcessarRelatorios() throws Exception {
        String token = token(1L, NivelAcesso.VOLUNTARIO);
        mockMvc.perform(get("/relatorios/fluxo-caixa").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void estagiarioNaoPodeCriarCategoria() throws Exception {
        String token = token(1L, NivelAcesso.ESTAGIARIO);
        mockMvc.perform(post("/categorias")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Material","tipo":"DESPESA"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void recrutandoNaoPodeAcessarFinanceiro() throws Exception {
        String token = token(1L, NivelAcesso.RECRUTANDO);
        mockMvc.perform(get("/custos-encomenda/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPodeCriarLancamento() throws Exception {
        Long idCategoria = seedCategoria("Material", TipoCategoriaFinanceira.DESPESA).getIdCategoria();
        String token = token(1L, NivelAcesso.ADMIN);
        mockMvc.perform(post("/lancamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idCategoria":%d,"tipo":"SAIDA","valor":100.00,
                                 "dataVencimento":"%s"}
                                """.formatted(idCategoria, LocalDate.now().plusDays(5))))
                .andExpect(status().isCreated());
    }

    @Test
    void tokenInvalidoRetorna401() throws Exception {
        mockMvc.perform(get("/lancamentos").header("Authorization", "Bearer token.invalido.aqui"))
                .andExpect(status().isUnauthorized());
    }
}