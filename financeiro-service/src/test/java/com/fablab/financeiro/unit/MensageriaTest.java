package com.fablab.financeiro.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fablab.financeiro.consumer.EncomendaCriadaListener;
import com.fablab.financeiro.consumer.HorasValidadasListener;
import com.fablab.financeiro.consumer.ProducaoConcluidaListener;
import com.fablab.financeiro.dto.CusteioDtos.CustoResponse;
import com.fablab.financeiro.dto.CusteioDtos.FechamentoResponse;
import com.fablab.financeiro.dto.FinanceiroEventos.EncomendaCriadaEvent;
import com.fablab.financeiro.dto.FinanceiroEventos.HorasValidadasEvent;
import com.fablab.financeiro.dto.FinanceiroEventos.ProducaoConcluidaEvent;
import com.fablab.financeiro.entity.StatusFechamento;
import com.fablab.financeiro.service.CusteioService;
import com.fablab.financeiro.service.FechamentoEncomendaService;
import com.fablab.financeiro.service.FinanceiroEventPublisher;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Mensageria: listeners delegam aos services e toleram eventos malformados. */
@ExtendWith(MockitoExtension.class)
class MensageriaTest {

    @Mock
    private FechamentoEncomendaService fechamentoService;
    @Mock
    private CusteioService custeioService;
    @Mock
    private FinanceiroEventPublisher publisher;

    @Test
    void encomendaCriadaAbreFechamento() {
        when(fechamentoService.criarDoEvento(any())).thenReturn(new FechamentoResponse(
                1L, 2051, BigDecimal.ZERO, new BigDecimal("1240.00"), LocalDate.now(),
                StatusFechamento.ABERTA, BigDecimal.ZERO));
        var listener = new EncomendaCriadaListener(fechamentoService);
        listener.onEncomendaCriada(new EncomendaCriadaEvent(2051, new BigDecimal("1240.00"), LocalDate.now()));
        org.mockito.Mockito.verify(fechamentoService).criarDoEvento(any());
    }

    @Test
    void eventoMalformadoNaoDerrubaConsumer() {
        var listener = new EncomendaCriadaListener(fechamentoService);
        listener.onEncomendaCriada(null);
        listener.onEncomendaCriada(new EncomendaCriadaEvent(null, null, null));
        org.mockito.Mockito.verifyNoInteractions(fechamentoService);

        var horasListener = new HorasValidadasListener(fechamentoService);
        horasListener.onHorasValidadas(null);
        horasListener.onHorasValidadas(new HorasValidadasEvent(null, null, null, null, null));
        org.mockito.Mockito.verifyNoInteractions(fechamentoService);
    }

    @Test
    void producaoConcluidaDisparaCusteio() {
        when(custeioService.calcularDoEvento(any())).thenReturn(new CustoResponse(
                1L, 2051, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("1240.00"), new BigDecimal("1240.00"), LocalDate.now()));
        var listener = new ProducaoConcluidaListener(custeioService);
        listener.onProducaoConcluida(new ProducaoConcluidaEvent(2051, LocalDate.now()));
        org.mockito.Mockito.verify(custeioService).calcularDoEvento(any());
    }

    @Test
    void horasValidadasAcumulam() {
        var listener = new HorasValidadasListener(fechamentoService);
        listener.onHorasValidadas(new HorasValidadasEvent(2051, 7, 1, new BigDecimal("2.00"), LocalDate.now()));
        org.mockito.Mockito.verify(fechamentoService).registrarHorasValidadas(any());
    }

    @Test
    void publicacoesRecuperamSeDeFalha() {
        org.springframework.amqp.rabbit.core.RabbitTemplate template =
                org.mockito.Mockito.mock(org.springframework.amqp.rabbit.core.RabbitTemplate.class);
        org.mockito.Mockito.doThrow(new RuntimeException("broker fora"))
                .when(template).convertAndSend(any(String.class), any(String.class), any(Object.class));
        var publisherReal = new FinanceiroEventPublisher(template);
        // não deve propagar: publisher captura e registra em log
        publisherReal.publishLancamentoVencido(new com.fablab.financeiro.dto.FinanceiroEventos.LancamentoVencidoEvent(
                1L, BigDecimal.ZERO, LocalDate.now(), null));
        publisherReal.publishCustoCalculado(new com.fablab.financeiro.dto.FinanceiroEventos.CustoCalculadoEvent(
                2051, BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now()));
        publisherReal.publishCompraSolicitada(new com.fablab.financeiro.dto.FinanceiroEventos.CompraSolicitadaEvent(
                1L, null));
        assertThat(publisherReal).isNotNull();
    }
}
