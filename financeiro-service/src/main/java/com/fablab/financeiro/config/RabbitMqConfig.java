package com.fablab.financeiro.config;

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
 * Configuração do RabbitMQ do Financeiro (exchange {@code fablab.financeiro},
 * filas próprias e consumo das exchanges de Vendas, RH e Produção).
 */
@Configuration
public class RabbitMqConfig {

    /** Exchange tópica do domínio financeiro (durável). */
    public static final String FINANCEIRO_EXCHANGE = "fablab.financeiro";

    /** Routing keys publicadas pelo Financeiro. */
    public static final String LANCAMENTO_VENCIDO_ROUTING_KEY = "lancamento.vencido.event";
    public static final String CUSTO_CALCULADO_ROUTING_KEY = "custo.calculado.event";
    public static final String COMPRA_SOLICITADA_ROUTING_KEY = "compra.solicitada.event";

    /** Exchanges dos domínios produtores consumidos. */
    public static final String VENDAS_EXCHANGE = "fablab.vendas";
    public static final String RH_EXCHANGE = "fablab.rh";
    public static final String PRODUCAO_EXCHANGE = "fablab.producao";

    /** Routing keys consumidas. */
    public static final String ENCOMENDA_CRIADA_ROUTING_KEY = "encomenda.criada.event";
    public static final String HORAS_VALIDADAS_ROUTING_KEY = "horas.validadas.event";
    public static final String PRODUCAO_CONCLUIDA_ROUTING_KEY = "producao.concluida.event";

    /** Filas próprias do Financeiro. */
    public static final String FECHAMENTO_QUEUE = "financeiro.fechamento.encomenda";
    public static final String HORAS_QUEUE = "financeiro.horas.encomenda";
    public static final String CUSTO_QUEUE = "financeiro.custo.calculo";

    private final ConnectionFactory connectionFactory;

    public RabbitMqConfig(@Value("${spring.rabbitmq.host:localhost}") String host,
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
        return template;
    }

    @Bean
    public TopicExchange financeiroExchange() {
        return new TopicExchange(FINANCEIRO_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange vendasExchange() {
        return new TopicExchange(VENDAS_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange rhExchange() {
        return new TopicExchange(RH_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange producaoExchange() {
        return new TopicExchange(PRODUCAO_EXCHANGE, true, false);
    }

    @Bean
    public Queue fechamentoQueue() {
        return QueueBuilder.durable(FECHAMENTO_QUEUE).build();
    }

    @Bean
    public Queue horasQueue() {
        return QueueBuilder.durable(HORAS_QUEUE).build();
    }

    @Bean
    public Queue custoQueue() {
        return QueueBuilder.durable(CUSTO_QUEUE).build();
    }

    @Bean
    public Binding fechamentoBinding(Queue fechamentoQueue, TopicExchange vendasExchange) {
        return BindingBuilder.bind(fechamentoQueue).to(vendasExchange).with(ENCOMENDA_CRIADA_ROUTING_KEY);
    }

    @Bean
    public Binding horasBinding(Queue horasQueue, TopicExchange rhExchange) {
        return BindingBuilder.bind(horasQueue).to(rhExchange).with(HORAS_VALIDADAS_ROUTING_KEY);
    }

    @Bean
    public Binding custoBinding(Queue custoQueue, TopicExchange producaoExchange) {
        return BindingBuilder.bind(custoQueue).to(producaoExchange).with(PRODUCAO_CONCLUIDA_ROUTING_KEY);
    }
}
