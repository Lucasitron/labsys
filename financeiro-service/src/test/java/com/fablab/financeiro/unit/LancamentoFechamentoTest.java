package com.fablab.financeiro.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.financeiro.dto.CusteioDtos.FechamentoRequest;
import com.fablab.financeiro.dto.FinanceiroEventos.EncomendaCriadaEvent;
import com.fablab.financeiro.dto.FinanceiroEventos.HorasValidadasEvent;
import com.fablab.financeiro.dto.LancamentoDtos.LancamentoListaResponse;
import com.fablab.financeiro.dto.LancamentoDtos.LancamentoRequest;
import com.fablab.financeiro.dto.LancamentoDtos.PagamentoRequest;
import com.fablab.financeiro.entity.CategoriaFinanceira;
import com.fablab.financeiro.entity.FechamentoEncomenda;
import com.fablab.financeiro.entity.HorasEncomenda;
import com.fablab.financeiro.entity.LancamentoFinanceiro;
import com.fablab.financeiro.entity.StatusFechamento;
import com.fablab.financeiro.entity.StatusLancamento;
import com.fablab.financeiro.entity.TipoCategoria;
import com.fablab.financeiro.entity.TipoLancamento;
import com.fablab.financeiro.repository.CategoriaFinanceiraRepository;
import com.fablab.financeiro.repository.FechamentoEncomendaRepository;
import com.fablab.financeiro.repository.HorasEncomendaRepository;
import com.fablab.financeiro.repository.LancamentoFinanceiroRepository;
import com.fablab.financeiro.service.FechamentoEncomendaService;
import com.fablab.financeiro.service.FinanceiroEventPublisher;
import com.fablab.financeiro.service.LancamentoFinanceiroService;
import com.fablab.financeiro.service.ManualPaymentProcessor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Regras de lançamentos e fechamento: status inicial, liquidação,
 * varredura de vencidos, idempotência e acúmulo de horas.
 */
@ExtendWith(MockitoExtension.class)
class LancamentoFechamentoTest {

    @Mock
    private LancamentoFinanceiroRepository lancamentoRepository;
    @Mock
    private CategoriaFinanceiraRepository categoriaRepository;
    @Mock
    private FinanceiroEventPublisher publisher;
    @Mock
    private FechamentoEncomendaRepository fechamentoRepository;
    @Mock
    private HorasEncomendaRepository horasRepository;

    private LancamentoFinanceiroService lancamentoService;
    private FechamentoEncomendaService fechamentoService;

    @BeforeEach
    void setup() {
        lancamentoService = new LancamentoFinanceiroService(lancamentoRepository,
                categoriaRepository, publisher, new ManualPaymentProcessor());
        fechamentoService = new FechamentoEncomendaService(fechamentoRepository, horasRepository);
    }

    private LancamentoRequest pedido(LocalDate vencimento) {
        return new LancamentoRequest(1L, TipoLancamento.SAIDA, new BigDecimal("100.00"),
                vencimento, "EN-1", null);
    }

    @Test
    void criarComVencimentoFuturoAssumePendenteSemEvento() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(new CategoriaFinanceira()));
        when(lancamentoRepository.save(any())).thenAnswer(i -> {
            LancamentoFinanceiro l = i.getArgument(0);
            l.setId(10L);
            return l;
        });

        var resposta = lancamentoService.criar(pedido(LocalDate.now().plusDays(5)));

        assertThat(resposta.status()).isEqualTo(StatusLancamento.PENDENTE);
        verify(publisher, org.mockito.Mockito.never()).publishLancamentoVencido(any());
    }

    @Test
    void criarVencidoAssumeAtrasadoEPublicaEvento() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(new CategoriaFinanceira()));
        when(lancamentoRepository.save(any())).thenAnswer(i -> {
            LancamentoFinanceiro l = i.getArgument(0);
            l.setId(11L);
            return l;
        });

        var resposta = lancamentoService.criar(pedido(LocalDate.now().minusDays(2)));

        assertThat(resposta.status()).isEqualTo(StatusLancamento.ATRASADO);
        verify(publisher).publishLancamentoVencido(any());
    }

    @Test
    void liquidarGravaPagamentoEPago() {
        LancamentoFinanceiro entity = new LancamentoFinanceiro();
        entity.setId(5L);
        entity.setStatus(StatusLancamento.PENDENTE);
        entity.setDataVencimento(LocalDate.now());
        when(lancamentoRepository.findById(5L)).thenReturn(Optional.of(entity));
        when(lancamentoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var resposta = lancamentoService.registrarPagamento(5L, new PagamentoRequest(LocalDate.now(), "pago via Pix"));

        assertThat(resposta.status()).isEqualTo(StatusLancamento.PAGO);
        assertThat(resposta.dataPagamento()).isNotNull();
    }

    @Test
    void liquidarPagoOuCanceladoRejeita() {
        for (StatusLancamento status : List.of(StatusLancamento.PAGO, StatusLancamento.CANCELADO)) {
            LancamentoFinanceiro entity = new LancamentoFinanceiro();
            entity.setId(6L);
            entity.setStatus(status);
            when(lancamentoRepository.findById(6L)).thenReturn(Optional.of(entity));
            assertThatThrownBy(() -> lancamentoService.registrarPagamento(6L, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void emitirVencidosPublicaPorLancamentoERetornaContagem() {
        LancamentoFinanceiro pendente = new LancamentoFinanceiro();
        pendente.setId(1L);
        pendente.setStatus(StatusLancamento.PENDENTE);
        pendente.setValor(new BigDecimal("50.00"));
        pendente.setDataVencimento(LocalDate.now().minusDays(1));
        LancamentoFinanceiro atrasado = new LancamentoFinanceiro();
        atrasado.setId(2L);
        atrasado.setStatus(StatusLancamento.ATRASADO);
        atrasado.setValor(new BigDecimal("70.00"));
        atrasado.setDataVencimento(LocalDate.now().minusDays(3));
        when(lancamentoRepository.findByStatusInAndDataVencimentoBefore(any(), any()))
                .thenReturn(List.of(pendente, atrasado));

        int publicados = lancamentoService.emitirVencidos();

        assertThat(publicados).isEqualTo(2);
        assertThat(pendente.getStatus()).isEqualTo(StatusLancamento.ATRASADO);
        verify(publisher, org.mockito.Mockito.times(2)).publishLancamentoVencido(any());
    }

    @Test
    void listarAplicaFiltrosEServeResumoECounts() {
        LancamentoFinanceiro entrada = new LancamentoFinanceiro();
        entrada.setId(1L);
        entrada.setIdCategoria(1L);
        entrada.setTipo(TipoLancamento.ENTRADA);
        entrada.setValor(new BigDecimal("1000.00"));
        entrada.setStatus(StatusLancamento.PAGO);
        entrada.setDataVencimento(LocalDate.now());
        LancamentoFinanceiro saida = new LancamentoFinanceiro();
        saida.setId(2L);
        saida.setIdCategoria(2L);
        saida.setTipo(TipoLancamento.SAIDA);
        saida.setValor(new BigDecimal("400.00"));
        saida.setStatus(StatusLancamento.PENDENTE);
        saida.setDataVencimento(LocalDate.now());
        saida.setIdReferenciaExterna("EN-2051");
        when(lancamentoRepository.findAll()).thenReturn(List.of(entrada, saida));

        LancamentoListaResponse resposta = lancamentoService.listar(
                StatusLancamento.PENDENTE, null, null, null, null, "EN-2051", null, null);

        assertThat(resposta.lancamentos()).hasSize(1);
        assertThat(resposta.counts().get("PENDENTE")).isEqualTo(1L);
        assertThat(resposta.resumo().saidas()).isEqualByComparingTo("400.00");
        assertThat(resposta.resumo().saldo()).isEqualByComparingTo("-400.00");
    }

    @Test
    void criarFechamentoDuplicadoRejeita() {
        when(fechamentoRepository.existsByIdEncomenda(2051)).thenReturn(true);
        assertThatThrownBy(() -> fechamentoService.criar(
                new FechamentoRequest(2051, new BigDecimal("8.0"), new BigDecimal("1240.00"), LocalDate.now()), 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void criarDoEventoEIdempotente() {
        EncomendaCriadaEvent event = new EncomendaCriadaEvent(2051, new BigDecimal("1240.00"), LocalDate.now());
        when(fechamentoRepository.findByIdEncomenda(2051)).thenReturn(Optional.empty());
        when(fechamentoRepository.save(any())).thenAnswer(i -> {
            FechamentoEncomenda f = i.getArgument(0);
            f.setId(1L);
            return f;
        });

        var primeira = fechamentoService.criarDoEvento(event);
        assertThat(primeira.status()).isEqualTo(StatusFechamento.ABERTA);
        assertThat(primeira.horasValidadas()).isEqualByComparingTo(BigDecimal.ZERO);

        FechamentoEncomenda existente = new FechamentoEncomenda();
        existente.setId(1L);
        existente.setIdEncomenda(2051);
        existente.setStatus(StatusFechamento.ABERTA);
        existente.setHorasEstimadas(BigDecimal.ZERO);
        existente.setValorFechado(new BigDecimal("1240.00"));
        existente.setDataFechamento(LocalDate.now());
        existente.setHorasValidadas(BigDecimal.ZERO);
        when(fechamentoRepository.findByIdEncomenda(2051)).thenReturn(Optional.of(existente));

        var segunda = fechamentoService.criarDoEvento(event);
        assertThat(segunda.id()).isEqualTo(1L);
        verify(fechamentoRepository, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void registrarHorasFazUpsertEAtualizaFechamento() {
        FechamentoEncomenda fechamento = new FechamentoEncomenda();
        fechamento.setId(1L);
        fechamento.setIdEncomenda(2051);
        fechamento.setStatus(StatusFechamento.ABERTA);
        fechamento.setHorasValidadas(new BigDecimal("2.00"));
        when(fechamentoRepository.findByIdEncomenda(2051)).thenReturn(Optional.of(fechamento));
        when(horasRepository.findByIdEncomendaAndIdFuncionarioAndDataRegistro(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(horasRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        fechamentoService.registrarHorasValidadas(
                new HorasValidadasEvent(2051, 7, 1, new BigDecimal("3.50"), LocalDate.now()));

        ArgumentCaptor<HorasEncomenda> captor = ArgumentCaptor.forClass(HorasEncomenda.class);
        verify(horasRepository).save(captor.capture());
        assertThat(captor.getValue().getHoras()).isEqualByComparingTo("3.50");
        assertThat(fechamento.getHorasValidadas()).isEqualByComparingTo("5.50");
    }
}
