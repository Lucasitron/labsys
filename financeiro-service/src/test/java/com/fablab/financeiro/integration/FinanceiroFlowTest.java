package com.fablab.financeiro.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.financeiro.TokenHelper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Fluxo ponta a ponta: categoria → lançamento → pagamento → doação →
 * valores/hora + overhead → fechamento → custo → relatórios.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FinanceiroFlowTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TokenHelper tokenHelper;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    private String admin() {
        return "Bearer " + tokenHelper.token(1L, "ADMIN");
    }

    private String criar(String url, String corpo) throws Exception {
        String resposta = mockMvc.perform(post(url)
                        .header(HttpHeaders.AUTHORIZATION, admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return resposta;
    }

    @Test
    void fluxoCompleto() throws Exception {
        String categoria = criar("/categorias",
                "{\"nome\":\"Vendas\",\"tipo\":\"RECEITA\",\"descricao\":\"Receitas\"}");
        long idCategoria = JsonPath.parse(categoria).read("$.id", Number.class).longValue();

        String lancamento = criar("/lancamentos",
                "{\"idCategoria\":" + idCategoria + ",\"tipo\":\"ENTRADA\",\"valor\":1240.00,"
                        + "\"dataVencimento\":\"2099-09-18\",\"idReferenciaExterna\":\"EN-2051\"}");
        long idLancamento = JsonPath.parse(lancamento).read("$.id", Number.class).longValue();
        assertThat(JsonPath.parse(lancamento).read("$.status", String.class)).isEqualTo("PENDENTE");

        mockMvc.perform(put("/lancamentos/" + idLancamento + "/pagamento")
                        .header(HttpHeaders.AUTHORIZATION, admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataPagamento\":\"2026-09-18\"}"))
                .andExpect(status().isOk());

        criar("/doacoes-recursos",
                "{\"tipo\":\"DOACAO\",\"origem\":\"Grupo Mãos que Criam\",\"valor\":1500.00,"
                        + "\"dataRecebimento\":\"2026-09-15\"}");

        criar("/valores-hora",
                "{\"nivelAcesso\":1,\"valorHora\":35.00,\"dataVigencia\":\"2026-10-01\"}");
        criar("/valores-hora",
                "{\"nivelAcesso\":2,\"valorHora\":10.00,\"dataVigencia\":\"2026-10-01\"}");
        criar("/parametros-overhead",
                "{\"valorTaxaHora\":12.00,\"dataVigencia\":\"2026-10-01\"}");

        criar("/fechamento-encomenda",
                "{\"idEncomenda\":2051,\"horasEstimadas\":8.0,\"valorFechado\":1240.00,"
                        + "\"dataFechamento\":\"2026-09-18\"}");

        mockMvc.perform(get("/lancamentos")
                        .header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/relatorios/fluxo-caixa")
                        .header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/relatorios/dre")
                        .header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/relatorios/lucratividade")
                        .header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/relatorios/inadimplencia")
                        .header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/relatorios/doacoes-despesas")
                        .header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/relatorios/custo-maquina")
                        .header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/relatorios/fluxo-caixa?periodo=2026-09")
                        .header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk());

        String compra = criar("/solicitacoes-compra",
                "{\"idItemEstoque\":10,\"quantidade\":5.00,\"valorEstimado\":248.90}");
        long idCompra = JsonPath.parse(compra).read("$.id", Number.class).longValue();
        mockMvc.perform(put("/solicitacoes-compra/" + idCompra + "/concluir")
                        .header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk());
    }

    @Test
    void validacaoRetornaFieldErrors() throws Exception {
        mockMvc.perform(post("/categorias")
                        .header(HttpHeaders.AUTHORIZATION, admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"tipo\":\"RECEITA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fechamentoDuplicadoRejeitado() throws Exception {
        criar("/fechamento-encomenda",
                "{\"idEncomenda\":9001,\"horasEstimadas\":4.0,\"valorFechado\":500.00}");
        mockMvc.perform(post("/fechamento-encomenda")
                        .header(HttpHeaders.AUTHORIZATION, admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEncomenda\":9001,\"horasEstimadas\":5.0,\"valorFechado\":600.00}"))
                .andExpect(status().isBadRequest());
    }
}
