package com.fablab.financeiro.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.fablab.financeiro.config.RabbitMqConfig;
import com.fablab.financeiro.dto.CompraSolicitadaEvent;
import com.fablab.financeiro.dto.CustoCalculadoEvent;
import com.fablab.financeiro.dto.LancamentoVencidoEvent;
import com.fablab.financeiro.entity.TipoLancamento;
import com.fablab.financeiro.service.FinanceiroEventPublisher;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class FinanceiroEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private FinanceiroEventPublisher publisher;

    @Test
    void publicaLancamentoVencido() {
        publisher.publishLancamentoVencido(new LancamentoVencidoEvent(
                1L, TipoLancamento.SAIDA, BigDecimal.TEN, LocalDate.now()));

        verify(rabbitTemplate).convertAndSend(eq(RabbitMqConfig.FINANCEIRO_EXCHANGE),
                eq(RabbitMqConfig.LANCAMENTO_VENCIDO_ROUTING_KEY), any(LancamentoVencidoEvent.class));
    }

    @Test
    void publicaCustoCalculado() {
        publisher.publishCustoCalculado(new CustoCalculadoEvent(1L, BigDecimal.TEN, BigDecimal.ONE));

        verify(rabbitTemplate).convertAndSend(eq(RabbitMqConfig.FINANCEIRO_EXCHANGE),
                eq(RabbitMqConfig.CUSTO_CALCULADO_ROUTING_KEY), any(CustoCalculadoEvent.class));
    }

    @Test
    void publicaCompraSolicitada() {
        publisher.publishCompraSolicitada(new CompraSolicitadaEvent(1L, null));

        verify(rabbitTemplate).convertAndSend(eq(RabbitMqConfig.FINANCEIRO_EXCHANGE),
                eq(RabbitMqConfig.COMPRA_SOLICITADA_ROUTING_KEY), any(CompraSolicitadaEvent.class));
    }

    @Test
    void falhaDePublicacaoNaoPropagaExcecao() {
        doThrow(new RuntimeException("broker indisponível"))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));

        assertDoesNotThrow(() -> publisher.publishCompraSolicitada(new CompraSolicitadaEvent(1L, null)));
    }
}