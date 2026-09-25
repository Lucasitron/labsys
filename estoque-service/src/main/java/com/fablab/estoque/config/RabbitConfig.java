package com.fablab.estoque.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do RabbitMQ do Estoque.
 *
 * <p>Define as exchanges tópicas {@code fablab.*}, as filas de consumo/serviço
 * (baixa automática da produção), a DLQ para mensagens com falha e os
 * indicadores de mensagem publicada (publisher confirms/returns).</p>
 */
@Configuration
public class RabbitConfig {

    /** Exchange tópica do domínio de estoque. */
    public static final String ESTOQUE_EXCHANGE = "fablab.estoque";

    /** Routing key do evento de estoque baixo. */
    public static final String ESTOQUE_BAIXO_ROUTING_KEY = "estoque.baixo.event";

    /** Routing key do evento de empréstimo em atraso. */
    public static final String EMPRESTIMO_ATRASADO_ROUTING_KEY = "emprestimo.atrasado.event";

    /** Routing key do evento de compra solicitada (entrada de estoque). */
    public static final String COMPRA_SOLICITADA_ROUTING_KEY = "compra.solicitada.event";

    /** Exchange tópica onde o Produção &amp; Projetos Service publica eventos. */
    public static final String PRODUCAO_EXCHANGE = "fablab.producao";

    /** Routing key do evento de produção concluída. */
    public static final String PRODUCAO_CONCLUIDA_ROUTING_KEY = "producao.concluida.event";

    /** Fila de baixa automática de estoque por BOM consumida pelo Estoque. */
    public static final String CONSUMO_BOM_QUEUE = "estoque.consumo.bom";

    /** Dead letter exchange para mensagens com falha de processamento. */
    public static final String ESTOQUE_DLX = "fablab.estoque.dlx";

    /** Fila de dead letters do consumo de BOM. */
    public static final String CONSUMO_BOM_DLQ = "estoque.consumo.bom.dlq";

    /** Exchange tópica onde o Financeiro Service publica eventos. */
    public static final String FINANCEIRO_EXCHANGE = "fablab.financeiro";

    private final ConnectionFactory connectionFactory;

    public RabbitConfig(@Value("${spring.rabbitmq.host:localhost}") String host,
                        @Value("${spring.rabbitmq.port:5672}") int port,
                        @Value("${spring.rabbitmq.username:fablab}") String username,
                        @Value("${spring.rabbitmq.password:fablab}") String password) {
        CachingConnectionFactory factory = new CachingConnectionFactory(host, port);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        factory.setPublisherReturns(true);
        this.connectionFactory = factory;
    }

    @Bean
    public ConnectionFactory rabbitConnectionFactory() {
        return connectionFactory;
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true);
        // Indicadores de mensagem publicada (publisher confirm/returns).
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                org.slf4j.LoggerFactory.getLogger(RabbitConfig.class)
                        .warn("Mensagem não confirmada pelo broker: {}", cause);
            }
        });
        template.setReturnsCallback(returned ->
                org.slf4j.LoggerFactory.getLogger(RabbitConfig.class)
                        .warn("Mensagem devolvida (rota não encontrada): {} -> {}",
                                returned.getExchange(), returned.getRoutingKey()));
        return template;
    }

    @Bean
    public TopicExchange estoqueExchange() {
        return new TopicExchange(ESTOQUE_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange producaoExchange() {
        return new TopicExchange(PRODUCAO_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange financeiroExchange() {
        return new TopicExchange(FINANCEIRO_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange estoqueDlx() {
        return new TopicExchange(ESTOQUE_DLX, true, false);
    }

    @Bean
    public Queue consumoBomQueue() {
        return QueueBuilder.durable(CONSUMO_BOM_QUEUE)
                .deadLetterExchange(ESTOQUE_DLX)
                .deadLetterRoutingKey(CONSUMO_BOM_DLQ)
                .build();
    }

    @Bean
    public Queue consumoBomDlq() {
        return QueueBuilder.durable(CONSUMO_BOM_DLQ).build();
    }

    @Bean
    public Binding consumoBomBinding(Queue consumoBomQueue, TopicExchange producaoExchange) {
        return BindingBuilder.bind(consumoBomQueue).to(producaoExchange).with(PRODUCAO_CONCLUIDA_ROUTING_KEY);
    }

    @Bean
    public Binding consumoBomDlqBinding(Queue consumoBomDlq, TopicExchange estoqueDlx) {
        return BindingBuilder.bind(consumoBomDlq).to(estoqueDlx).with(CONSUMO_BOM_DLQ);
    }
}