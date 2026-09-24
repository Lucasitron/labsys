package com.fablab.producao.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.fablab.producao.config.RabbitMqConfig;
import com.fablab.producao.dto.AdvertenciaLimiteEvent;
import com.fablab.producao.dto.AdvertenciaRegistradaEvent;
import com.fablab.producao.dto.KanbanStatusAlteradoEvent;
import com.fablab.producao.dto.ProducaoConcluidaEvent;
import com.fablab.producao.dto.ProducaoStatusEvent;
import com.fablab.producao.dto.ProjetoMesaAbandonadoEvent;
import com.fablab.producao.entity.KanbanStatus;
import com.fablab.producao.service.ProducaoEventPublisher;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class ProducaoEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ProducaoEventPublisher publisher;

    @Test
    void publicaStatusAlterado() {
        publisher.publicarStatusAlterado(new ProducaoStatusEvent(1L, "PRODUCAO", 5L, "obs"));

        verify(rabbitTemplate).convertAndSend(eq(RabbitMqConfig.PRODUCAO_EXCHANGE),
                eq(RabbitMqConfig.PRODUCAO_STATUS_ALTERADO_ROUTING_KEY), any(ProducaoStatusEvent.class));
    }

    @Test
    void publicaKanbanStatusAlterado() {
        publisher.publicarKanbanStatusAlterado(new KanbanStatusAlteradoEvent(
                1L, KanbanStatus.FILA, KanbanStatus.PRODUCAO, LocalDateTime.now()));

        verify(rabbitTemplate).convertAndSend(eq(RabbitMqConfig.NOTIFICACAO_EXCHANGE),
                eq(RabbitMqConfig.KANBAN_STATUS_ALTERADO_ROUTING_KEY), any(KanbanStatusAlteradoEvent.class));
    }

    @Test
    void publicaProducaoConcluida() {
        publisher.publicarProducaoConcluida(new ProducaoConcluidaEvent(1L, null, List.of(), null));

        verify(rabbitTemplate).convertAndSend(eq(RabbitMqConfig.PRODUCAO_EXCHANGE),
                eq(RabbitMqConfig.PRODUCAO_CONCLUIDA_ROUTING_KEY), any(ProducaoConcluidaEvent.class));
    }

    @Test
    void publicaAdvertencia() {
        publisher.publicarAdvertencia(new AdvertenciaRegistradaEvent(5L, 1, "motivo"));

        verify(rabbitTemplate).convertAndSend(eq(RabbitMqConfig.NOTIFICACAO_EXCHANGE),
                eq(RabbitMqConfig.ADVERTENCIA_REGISTRADA_ROUTING_KEY), any(AdvertenciaRegistradaEvent.class));
    }

    @Test
    void publicaAdvertenciaLimite() {
        publisher.publicarAdvertenciaLimite(new AdvertenciaLimiteEvent(5L, 3, "motivo"));

        verify(rabbitTemplate).convertAndSend(eq(RabbitMqConfig.NOTIFICACAO_EXCHANGE),
                eq(RabbitMqConfig.ADVERTENCIA_LIMITE_ROUTING_KEY), any(AdvertenciaLimiteEvent.class));
    }

    @Test
    void publicaProjetoMesaAbandonado() {
        publisher.publicarProjetoMesaAbandonado(new ProjetoMesaAbandonadoEvent(1L, 5L, "Recolher"));

        verify(rabbitTemplate).convertAndSend(eq(RabbitMqConfig.NOTIFICACAO_EXCHANGE),
                eq(RabbitMqConfig.PROJETO_MESA_ABANDONADO_ROUTING_KEY), any(ProjetoMesaAbandonadoEvent.class));
    }
}