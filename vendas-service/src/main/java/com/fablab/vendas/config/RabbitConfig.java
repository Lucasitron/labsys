package com.fablab.vendas.config;

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
 * Configuração do RabbitMQ do Vendas (exchange {@code fablab.vendas},
 * publicadores de encomenda/orçamento e consumo idempotente do Kanban do
 * Produção, com DLQ).
 */
@Configuration
public class RabbitConfig {

    /** Exchange tópica do domínio de vendas. */
    public static final String VENDAS_EXCHANGE = "fablab.vendas";

    /** Routing key do evento de encomenda criada. */
    public static final String ENCOMENDA_CRIADA_ROUTING_KEY = "encomenda.criada.event";

    /** Routing key do evento de status de encomenda alterado. */
    public static final String ENCOMENDA_STATUS_ROUTING_KEY = "encomenda.status.alterado.event";

    /** Routing key do evento de orçamento aprovado. */
    public static final String ORCAMENTO_APROVADO_ROUTING_KEY = "orcamento.aprovado.event";

    /** Exchange tópica onde o Produção &amp; Projetos Service publica eventos. */
    public static final String PRODUCAO_EXCHANGE = "fablab.producao";

    /** Routing key do evento de status alterado na produção. */
    public static final String PRODUCAO_STATUS_ROUTING_KEY = "producao.status.alterado.event";

    /** Fila de sincronização do Kanban consumida pelo Vendas. */
    public static final String PRODUCAO_STATUS_QUEUE = "vendas.producao.status";

    /** Dead letter exchange para mensagens com falha de processamento. */
    public static final String VENDAS_DLX = "fablab.vendas.dlx";

    /** Fila de dead letters da sincronização com a produção. */
    public static final String PRODUCAO_STATUS_DLQ = "vendas.producao.status.dlq";

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
    public TopicExchange vendasExchange() {
        return new TopicExchange(VENDAS_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange producaoExchange() {
        return new TopicExchange(PRODUCAO_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange vendasDlx() {
        return new TopicExchange(VENDAS_DLX, true, false);
    }

    @Bean
    public Queue producaoStatusQueue() {
        return QueueBuilder.durable(PRODUCAO_STATUS_QUEUE)
                .deadLetterExchange(VENDAS_DLX)
                .deadLetterRoutingKey(PRODUCAO_STATUS_DLQ)
                .build();
    }

    @Bean
    public Queue producaoStatusDlq() {
        return QueueBuilder.durable(PRODUCAO_STATUS_DLQ).build();
    }

    @Bean
    public Binding producaoStatusBinding(Queue producaoStatusQueue, TopicExchange producaoExchange) {
        return BindingBuilder.bind(producaoStatusQueue).to(producaoExchange).with(PRODUCAO_STATUS_ROUTING_KEY);
    }

    @Bean
    public Binding producaoStatusDlqBinding(Queue producaoStatusDlq, TopicExchange vendasDlx) {
        return BindingBuilder.bind(producaoStatusDlq).to(vendasDlx).with(PRODUCAO_STATUS_DLQ);
    }
}
