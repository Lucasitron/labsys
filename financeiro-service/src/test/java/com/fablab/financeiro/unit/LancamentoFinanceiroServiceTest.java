package com.fablab.financeiro.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.financeiro.dto.LancamentoFinanceiroRequest;
import com.fablab.financeiro.dto.LancamentoVencidoEvent;
import com.fablab.financeiro.entity.CategoriaFinanceira;
import com.fablab.financeiro.entity.LancamentoFinanceiro;
import com.fablab.financeiro.entity.StatusLancamento;
import com.fablab.financeiro.entity.TipoCategoriaFinanceira;
import com.fablab.financeiro.entity.TipoLancamento;
import com.fablab.financeiro.exception.ResourceNotFoundException;
import com.fablab.financeiro.repository.CategoriaFinanceiraRepository;
import com.fablab.financeiro.repository.LancamentoFinanceiroRepository;
import com.fablab.financeiro.service.FinanceiroEventPublisher;
import com.fablab.financeiro.service.LancamentoFinanceiroService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LancamentoFinanceiroServiceTest {

    @Mock
    private LancamentoFinanceiroRepository lancamentoRepository;
    @Mock
    private CategoriaFinanceiraRepository categoriaRepository;
    @Mock
    private FinanceiroEventPublisher eventPublisher;

    @InjectMocks
    private LancamentoFinanceiroService service;

    private CategoriaFinanceira categoria() {
        CategoriaFinanceira c = new CategoriaFinanceira();
        c.setIdCategoria(1L);
        c.setNome("Material");
        c.setTipo(TipoCategoriaFinanceira.DESPESA);
        return c;
    }

    private LancamentoFinanceiro lancamento(Long id, StatusLancamento status,
                                            TipoLancamento tipo, LocalDate vencimento) {
        LancamentoFinanceiro l = new LancamentoFinanceiro();
        l.setIdLancamento(id);
        l.setCategoria(categoria());
        l.setStatus(status);
        l.setTipo(tipo);
        l.setValor(BigDecimal.TEN);
        l.setDataVencimento(vencimento);
        return l;
    }

    @Test
    void criarComVencimentoFuturoFicaPendenteSemPublicar() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria()));
        when(lancamentoRepository.save(any(LancamentoFinanceiro.class))).thenAnswer(inv -> {
            LancamentoFinanceiro l = inv.getArgument(0);
            l.setIdLancamento(1L);
            return l;
        });

        var response = service.criar(new LancamentoFinanceiroRequest(
                1L, TipoLancamento.SAIDA, BigDecimal.TEN, LocalDate.now().plusDays(5), "10", "nota"));

        assertEquals(StatusLancamento.PENDENTE, response.status());
        verify(eventPublisher, never()).publishLancamentoVencido(any());
    }

    @Test
    void criarComVencimentoPassadoFicaAtrasadoEPublica() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria()));
        when(lancamentoRepository.save(any(LancamentoFinanceiro.class))).thenAnswer(inv -> {
            LancamentoFinanceiro l = inv.getArgument(0);
            l.setIdLancamento(2L);
            return l;
        });

        var response = service.criar(new LancamentoFinanceiroRequest(
                1L, TipoLancamento.ENTRADA, BigDecimal.TEN, LocalDate.now().minusDays(1), null, null));

        assertEquals(StatusLancamento.ATRASADO, response.status());
        verify(eventPublisher).publishLancamentoVencido(any(LancamentoVencidoEvent.class));
    }

    @Test
    void criarComCategoriaInexistenteLancaErro() {
        when(categoriaRepository.findById(9L)).thenReturn(Optional.empty());
        var request = new LancamentoFinanceiroRequest(
                9L, TipoLancamento.SAIDA, BigDecimal.TEN, LocalDate.now().plusDays(1), null, null);
        assertThrows(ResourceNotFoundException.class, () -> service.criar(request));
    }

    @Test
    void listarPorStatus() {
        when(lancamentoRepository.findByStatus(StatusLancamento.PENDENTE))
                .thenReturn(List.of(lancamento(1L, StatusLancamento.PENDENTE, TipoLancamento.SAIDA, LocalDate.now())));
        assertEquals(1, service.listar(StatusLancamento.PENDENTE, null, null, null).size());
    }

    @Test
    void listarPorPeriodoSemInicioUsaEpoca() {
        when(lancamentoRepository.findByDataVencimentoBetween(any(), any()))
                .thenReturn(List.of(lancamento(1L, StatusLancamento.PENDENTE, TipoLancamento.SAIDA, LocalDate.now())));
        var lista = service.listar(null, null, LocalDate.now(), null);
        assertEquals(1, lista.size());
        verify(lancamentoRepository).findByDataVencimentoBetween(eq(LocalDate.EPOCH), any());
    }

    @Test
    void listarPorCategoria() {
        when(lancamentoRepository.findByCategoria_IdCategoria(1L))
                .thenReturn(List.of(lancamento(1L, StatusLancamento.PENDENTE, TipoLancamento.SAIDA, LocalDate.now())));
        assertEquals(1, service.listar(null, null, null, 1L).size());
    }

    @Test
    void listarSemFiltrosRetornaTodos() {
        when(lancamentoRepository.findAll())
                .thenReturn(List.of(lancamento(1L, StatusLancamento.PENDENTE, TipoLancamento.SAIDA, LocalDate.now())));
        assertEquals(1, service.listar(null, null, null, null).size());
    }

    @Test
    void registrarPagamentoLiquidaLancamento() {
        var l = lancamento(1L, StatusLancamento.PENDENTE, TipoLancamento.SAIDA, LocalDate.now());
        when(lancamentoRepository.findById(1L)).thenReturn(Optional.of(l));
        when(lancamentoRepository.save(l)).thenReturn(l);

        var response = service.registrarPagamento(1L);

        assertEquals(StatusLancamento.PAGO, response.status());
    }

    @Test
    void registrarPagamentoRejeitaLancamentoJaPago() {
        var l = lancamento(1L, StatusLancamento.PAGO, TipoLancamento.SAIDA, LocalDate.now());
        when(lancamentoRepository.findById(1L)).thenReturn(Optional.of(l));
        assertThrows(IllegalArgumentException.class, () -> service.registrarPagamento(1L));
    }

    @Test
    void registrarPagamentoRejeitaLancamentoInexistente() {
        when(lancamentoRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.registrarPagamento(1L));
    }

    @Test
    void emitirVencidosPublicaEventos() {
        when(lancamentoRepository.findByStatusInAndDataVencimentoBefore(any(), any()))
                .thenReturn(List.of(
                        lancamento(1L, StatusLancamento.PENDENTE, TipoLancamento.SAIDA, LocalDate.now().minusDays(1)),
                        lancamento(2L, StatusLancamento.ATRASADO, TipoLancamento.ENTRADA, LocalDate.now().minusDays(2))));

        long total = service.emitirVencidos();

        assertEquals(2, total);
        verify(eventPublisher, times(2)).publishLancamentoVencido(any(LancamentoVencidoEvent.class));
    }
}