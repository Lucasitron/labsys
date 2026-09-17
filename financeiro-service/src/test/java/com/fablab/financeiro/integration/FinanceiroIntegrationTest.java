package com.fablab.financeiro.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fablab.financeiro.config.RabbitMqConfig;
import com.fablab.financeiro.dto.LancamentoVencidoEvent;
import com.fablab.financeiro.entity.CategoriaFinanceira;
import com.fablab.financeiro.entity.FechamentoEncomenda;
import com.fablab.financeiro.entity.HorasEncomenda;
import com.fablab.financeiro.entity.LancamentoFinanceiro;
import com.fablab.financeiro.entity.NivelAcesso;
import com.fablab.financeiro.entity.ParametroOverhead;
import com.fablab.financeiro.entity.SolicitacaoCompra;
import com.fablab.financeiro.entity.StatusFechamentoEncomenda;
import com.fablab.financeiro.entity.StatusLancamento;
import com.fablab.financeiro.entity.StatusSolicitacaoCompra;
import com.fablab.financeiro.entity.TipoCategoriaFinanceira;
import com.fablab.financeiro.entity.TipoLancamento;
import com.fablab.financeiro.entity.ValorHoraNivel;
import com.fablab.financeiro.service.CustoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class FinanceiroIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CustoService custoService;

    private String admin() {
        return token(1L, NivelAcesso.ADMIN);
    }

    private LancamentoFinanceiro seedLancamento(CategoriaFinanceira categoria, TipoLancamento tipo,
                                                double valor, LocalDate vencimento, String referencia,
                                                StatusLancamento status) {
        LancamentoFinanceiro l = new LancamentoFinanceiro();
        l.setCategoria(categoria);
        l.setTipo(tipo);
        l.setValor(BigDecimal.valueOf(valor));
        l.setDataVencimento(vencimento);
        l.setIdReferenciaExterna(referencia);
        l.setStatus(status);
        return lancamentoRepository.save(l);
    }

    private FechamentoEncomenda seedFechamento(Long idEncomenda, double valor) {
        FechamentoEncomenda f = new FechamentoEncomenda();
        f.setIdEncomenda(idEncomenda);
        f.setValorFechado(BigDecimal.valueOf(valor));
        f.setDataFechamento(LocalDate.now());
        f.setStatus(StatusFechamentoEncomenda.ABERTA);
        f.setHorasValidadas(BigDecimal.ZERO);
        return fechamentoRepository.save(f);
    }

    @Test
    void fluxoDeCategoriasELancamentos() throws Exception {
        mockMvc.perform(post("/categorias")
                        .header("Authorization", "Bearer " + admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Material","tipo":"DESPESA","descricao":"Insumos"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Material"));

        mockMvc.perform(post("/categorias")
                        .header("Authorization", "Bearer " + admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Material","tipo":"DESPESA"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/categorias").param("tipo", "DESPESA")
                        .header("Authorization", "Bearer " + admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Material"));

        CategoriaFinanceira categoria = categoriaRepository.findAll().get(0);

        mockMvc.perform(post("/lancamentos")
                        .header("Authorization", "Bearer " + admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idCategoria":%d,"tipo":"SAIDA","valor":150.00,
                                 "dataVencimento":"%s","idReferenciaExterna":"10"}
                                """.formatted(categoria.getIdCategoria(), LocalDate.now().plusDays(10))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"));

        Long idLancamento = lancamentoRepository.findAll().get(0).getIdLancamento();

        mockMvc.perform(get("/lancamentos").header("Authorization", "Bearer " + admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idLancamento").value(idLancamento));

        mockMvc.perform(get("/lancamentos").param("status", "PENDENTE")
                        .header("Authorization", "Bearer " + admin()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/lancamentos")
                        .param("dataInicio", LocalDate.now().toString())
                        .param("dataFim", LocalDate.now().plusDays(30).toString())
                        .header("Authorization", "Bearer " + admin()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/lancamentos").param("idCategoria", categoria.getIdCategoria().toString())
                        .header("Authorization", "Bearer " + admin()))
                .andExpect(status().isOk());

        mockMvc.perform(put("/lancamentos/{id}/pagamento", idLancamento)
                        .header("Authorization", "Bearer " + admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAGO"));
    }

    @Test
    void lancamentoVencidoFicaAtrasadoEPublicaEvento() throws Exception {
        CategoriaFinanceira categoria = seedCategoria("Aluguel", TipoCategoriaFinanceira.DESPESA);

        mockMvc.perform(post("/lancamentos")
                        .header("Authorization", "Bearer " + admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idCategoria":%d,"tipo":"SAIDA","valor":90.00,
                                 "dataVencimento":"%s"}
                                """.formatted(categoria.getIdCategoria(), LocalDate.now().minusDays(2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ATRASADO"));

        verify(rabbitTemplate).convertAndSend(eq(RabbitMqConfig.FINANCEIRO_EXCHANGE),
                eq(RabbitMqConfig.LANCAMENTO_VENCIDO_ROUTING_KEY), any(LancamentoVencidoEvent.class));
    }

    @Test
    void doacoesRecursos() throws Exception {
        mockMvc.perform(post("/doacoes-recursos")
                        .header("Authorization", "Bearer " + admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"DOACAO","origem":"Empresa X","valor":500.00,
                                 "dataRecebimento":"%s"}
                                """.formatted(LocalDate.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("DOACAO"));

        mockMvc.perform(get("/doacoes-recursos")
                        .param("tipo", "DOACAO")
                        .header("Authorization", "Bearer " + admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].origem").value("Empresa X"));
    }

    @Test
    void valoresHoraEParametroOverhead() throws Exception {
        mockMvc.perform(post("/valores-hora")
                        .header("Authorization", "Bearer " + admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nivelAcesso":1,"valorHora":50.00,"dataVigencia":"%s"}
                                """.formatted(LocalDate.now())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/valores-hora").header("Authorization", "Bearer " + admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nivelAcesso").value(1));

        mockMvc.perform(post("/parametros-overhead")
                        .header("Authorization", "Bearer " + admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"valorTaxaHora":5.00,"dataVigencia":"%s"}
                                """.formatted(LocalDate.now())))
                .andExpect(status().isCreated());
    }

    @Test
    void fechamentoEncomenda() throws Exception {
        mockMvc.perform(post("/fechamento-encomenda")
                        .header("Authorization", "Bearer " + admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idEncomenda":50,"horasEstimadas":10.00,"valorFechado":800.00,
                                 "dataFechamento":"%s"}
                                """.formatted(LocalDate.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ABERTA"));

        mockMvc.perform(get("/fechamento-encomenda/50").header("Authorization", "Bearer " + admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorFechado").value(800.00));

        mockMvc.perform(post("/fechamento-encomenda")
                        .header("Authorization", "Bearer " + admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idEncomenda":50,"valorFechado":100.00,"dataFechamento":"%s"}
                                """.formatted(LocalDate.now())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fechamentoInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/fechamento-encomenda/999").header("Authorization", "Bearer " + admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void solicitacaoDeCompra() throws Exception {
        mockMvc.perform(post("/solicitacoes-compra")
                        .header("Authorization", "Bearer " + admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idItemEstoque":1,"quantidade":3.00,"valorEstimado":300.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REGISTRADA"));

        SolicitacaoCompra solicitacao = solicitacaoRepository.findAll().get(0);

        mockMvc.perform(put("/solicitacoes-compra/{id}/concluir", solicitacao.getIdSolicitacao())
                        .header("Authorization", "Bearer " + admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"));
    }

    @Test
    void calculoDeCustoEConsultasDeRelatorio() {
        CategoriaFinanceira categoria = seedCategoria("Material", TipoCategoriaFinanceira.DESPESA);
        seedFechamento(42L, 1000);
        ValorHoraNivel valorHora = new ValorHoraNivel();
        valorHora.setNivelAcesso(2);
        valorHora.setValorHora(BigDecimal.valueOf(20));
        valorHora.setDataVigencia(LocalDate.now());
        valorHoraRepository.save(valorHora);
        ParametroOverhead overhead = new ParametroOverhead();
        overhead.setValorTaxaHora(BigDecimal.valueOf(5));
        overhead.setDataVigencia(LocalDate.now());
        parametroRepository.save(overhead);
        seedLancamento(categoria, TipoLancamento.SAIDA, 200, LocalDate.now(), "42", StatusLancamento.PAGO);
        HorasEncomenda horas = new HorasEncomenda();
        horas.setIdEncomenda(42L);
        horas.setIdFuncionario(1L);
        horas.setNivelAcesso(2);
        horas.setHoras(BigDecimal.valueOf(3));
        horas.setDataRegistro(LocalDate.now());
        horasRepository.save(horas);
        seedLancamento(categoria, TipoLancamento.ENTRADA, 50, LocalDate.now().minusDays(3), null,
                StatusLancamento.ATRASADO);

        var response = custoService.calcularCusto(42L);

        org.junit.jupiter.api.Assertions.assertEquals(
                0, BigDecimal.valueOf(275).compareTo(response.custoTotal()));
        org.junit.jupiter.api.Assertions.assertEquals(
                0, BigDecimal.valueOf(725).compareTo(response.margemLucro()));
    }

    @Test
    void endpointsDeRelatorioRespondem() throws Exception {
        CategoriaFinanceira categoria = seedCategoria("Servico", TipoCategoriaFinanceira.RECEITA);
        seedFechamento(1L, 500);
        seedLancamento(categoria, TipoLancamento.ENTRADA, 100, LocalDate.now(), "maquina-1",
                StatusLancamento.PENDENTE);

        mockMvc.perform(get("/relatorios/fluxo-caixa").header("Authorization", "Bearer " + admin()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/relatorios/dre")
                        .param("dataInicio", LocalDate.now().minusDays(1).toString())
                        .param("dataFim", LocalDate.now().plusDays(1).toString())
                        .header("Authorization", "Bearer " + admin()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/relatorios/lucratividade").header("Authorization", "Bearer " + admin()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/relatorios/inadimplencia").header("Authorization", "Bearer " + admin()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/relatorios/doacoes-despesas").header("Authorization", "Bearer " + admin()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/relatorios/custo-maquina").header("Authorization", "Bearer " + admin()))
                .andExpect(status().isOk());
    }

    @Test
    void custoInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/custos-encomenda/123").header("Authorization", "Bearer " + admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void requisicaoInvalidaRetorna400() throws Exception {
        mockMvc.perform(post("/categorias")
                        .header("Authorization", "Bearer " + admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"","tipo":null}
                                """))
                .andExpect(status().isBadRequest());
    }
}