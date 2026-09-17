package com.fablab.financeiro.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.fablab.financeiro.entity.CustoEncomenda;
import com.fablab.financeiro.entity.LancamentoFinanceiro;
import com.fablab.financeiro.entity.StatusLancamento;
import com.fablab.financeiro.entity.TipoDoacaoRecurso;
import com.fablab.financeiro.entity.TipoLancamento;
import com.fablab.financeiro.repository.CustoEncomendaRepository;
import com.fablab.financeiro.repository.DoacaoRecursoRepository;
import com.fablab.financeiro.repository.LancamentoFinanceiroRepository;
import com.fablab.financeiro.service.RelatorioService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    @Mock
    private LancamentoFinanceiroRepository lancamentoRepository;
    @Mock
    private DoacaoRecursoRepository doacaoRepository;
    @Mock
    private CustoEncomendaRepository custoRepository;

    @InjectMocks
    private RelatorioService service;

    private LancamentoFinanceiro lancamento(TipoLancamento tipo, StatusLancamento status,
                                            String referencia, double valor) {
        LancamentoFinanceiro l = new LancamentoFinanceiro();
        l.setTipo(tipo);
        l.setStatus(status);
        l.setIdReferenciaExterna(referencia);
        l.setValor(BigDecimal.valueOf(valor));
        l.setDataVencimento(LocalDate.now().minusDays(1));
        return l;
    }

    private CustoEncomenda custo(double venda, double total, double margem) {
        CustoEncomenda c = new CustoEncomenda();
        c.setIdEncomenda(1L);
        c.setValorVenda(BigDecimal.valueOf(venda));
        c.setCustoTotal(BigDecimal.valueOf(total));
        c.setMargemLucro(BigDecimal.valueOf(margem));
        c.setDataCalculo(LocalDate.now());
        return c;
    }

    @Test
    void fluxoCaixaCalculaSaldo() {
        when(lancamentoRepository.somaValoresPorTipo(eq(TipoLancamento.ENTRADA), any(), any()))
                .thenReturn(BigDecimal.valueOf(100));
        when(lancamentoRepository.somaValoresPorTipo(eq(TipoLancamento.SAIDA), any(), any()))
                .thenReturn(BigDecimal.valueOf(40));

        var response = service.fluxoCaixa(LocalDate.now().minusDays(1), LocalDate.now());

        assertEquals(0, BigDecimal.valueOf(60).compareTo(response.saldo()));
    }

    @Test
    void dreSomaDoacoesAsReceitas() {
        when(lancamentoRepository.somaValoresPorTipo(eq(TipoLancamento.ENTRADA), any(), any()))
                .thenReturn(BigDecimal.valueOf(100));
        when(lancamentoRepository.somaValoresPorTipo(eq(TipoLancamento.SAIDA), any(), any()))
                .thenReturn(BigDecimal.valueOf(40));
        when(doacaoRepository.somarValores(isNull(), any(), any())).thenReturn(BigDecimal.valueOf(30));

        var response = service.dre(LocalDate.now().minusDays(1), LocalDate.now());

        assertEquals(0, BigDecimal.valueOf(130).compareTo(response.receitas()));
        assertEquals(0, BigDecimal.valueOf(90).compareTo(response.resultado()));
    }

    @Test
    void lucratividadeCalculaPercentual() {
        when(custoRepository.findAllByOrderByDataCalculoDesc()).thenReturn(List.of(custo(200, 150, 50)));

        var lista = service.lucratividade();

        assertEquals(1, lista.size());
        assertEquals(0, BigDecimal.valueOf(25).compareTo(lista.get(0).margemPercentual()));
    }

    @Test
    void lucratividadeSemVendaZeraPercentual() {
        when(custoRepository.findAllByOrderByDataCalculoDesc()).thenReturn(List.of(custo(0, 10, -10)));

        var lista = service.lucratividade();

        assertEquals(0, BigDecimal.ZERO.compareTo(lista.get(0).margemPercentual()));
    }

    @Test
    void inadimplenciaConsideraSomenteEntradas() {
        LancamentoFinanceiro aReceber = lancamento(TipoLancamento.ENTRADA, StatusLancamento.ATRASADO, null, 50);
        aReceber.setCategoria(new com.fablab.financeiro.entity.CategoriaFinanceira());
        LancamentoFinanceiro aPagar = lancamento(TipoLancamento.SAIDA, StatusLancamento.ATRASADO, null, 30);
        when(lancamentoRepository.findByStatusInAndDataVencimentoBefore(any(), any()))
                .thenReturn(List.of(aReceber, aPagar));

        var lista = service.inadimplencia();

        assertEquals(1, lista.size());
    }

    @Test
    void doacoesDespesasCalculaSaldo() {
        when(doacaoRepository.somarValores(eq(TipoDoacaoRecurso.DOACAO), any(), any()))
                .thenReturn(BigDecimal.valueOf(30));
        when(lancamentoRepository.somaValoresPorTipo(eq(TipoLancamento.SAIDA), any(), any()))
                .thenReturn(BigDecimal.valueOf(40));

        var response = service.doacoesDespesas(LocalDate.now().minusDays(1), LocalDate.now());

        assertEquals(0, BigDecimal.valueOf(-10).compareTo(response.saldo()));
    }

    @Test
    void custoMaquinaAgrupaSaidasValidas() {
        when(lancamentoRepository.findAll()).thenReturn(List.of(
                lancamento(TipoLancamento.SAIDA, StatusLancamento.PAGO, "maquina-1", 100),
                lancamento(TipoLancamento.SAIDA, StatusLancamento.PENDENTE, "maquina-1", 50),
                lancamento(TipoLancamento.SAIDA, StatusLancamento.PAGO, "maquina-2", 20),
                lancamento(TipoLancamento.SAIDA, StatusLancamento.PAGO, null, 99),
                lancamento(TipoLancamento.SAIDA, StatusLancamento.PAGO, "  ", 5),
                lancamento(TipoLancamento.ENTRADA, StatusLancamento.PAGO, "maquina-3", 7),
                lancamento(TipoLancamento.SAIDA, StatusLancamento.CANCELADO, "maquina-4", 3)));

        var lista = service.custoMaquina();

        assertEquals(2, lista.size());
        assertEquals("maquina-1", lista.get(0).referenciaExterna());
        assertEquals(0, BigDecimal.valueOf(150).compareTo(lista.get(0).custoTotal()));
    }
}