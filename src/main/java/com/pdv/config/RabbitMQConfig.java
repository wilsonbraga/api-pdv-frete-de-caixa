package com.pdv.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do RabbitMQ — HABILITADO.
 * RabbitMQ rodando localmente na porta 5672 (admin: localhost:15672)
 */
@Configuration
public class RabbitMQConfig {

    // ===== NOMES =====
    public static final String EXCHANGE_PDV        = "pdv.exchange";
    public static final String EXCHANGE_PDV_DLX    = "pdv.exchange.dlx";

    public static final String QUEUE_SYNC          = "pdv.sync.queue";
    public static final String QUEUE_SYNC_DLQ      = "pdv.sync.queue.dlq";
    public static final String QUEUE_NOTIFICACAO   = "pdv.notificacao.queue";
    public static final String QUEUE_PIX           = "pdv.pix.queue";
    public static final String QUEUE_PIX_DLQ       = "pdv.pix.queue.dlq";

    public static final String ROUTING_SYNC        = "pdv.sync";
    public static final String ROUTING_NOTIFICACAO = "pdv.notificacao";
    public static final String ROUTING_PIX         = "pdv.pix";

    // ===== EXCHANGE PRINCIPAL =====
    @Bean
    public TopicExchange pdvExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_PDV).durable(true).build();
    }

    // ===== DEAD LETTER EXCHANGE =====
    @Bean
    public DirectExchange pdvDlxExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE_PDV_DLX).durable(true).build();
    }

    // ===== QUEUES =====
    @Bean
    public Queue syncQueue() {
        return QueueBuilder.durable(QUEUE_SYNC)
                .withArgument("x-dead-letter-exchange", EXCHANGE_PDV_DLX)
                .withArgument("x-dead-letter-routing-key", QUEUE_SYNC_DLQ)
                .withArgument("x-message-ttl", 86400000)
                .build();
    }

    @Bean
    public Queue syncDlQueue() {
        return QueueBuilder.durable(QUEUE_SYNC_DLQ).build();
    }

    @Bean
    public Queue notificacaoQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFICACAO).build();
    }

    // Fila dedicada para confirmações PIX do Mercado Pago
    @Bean
    public Queue pixQueue() {
        return QueueBuilder.durable(QUEUE_PIX)
                .withArgument("x-dead-letter-exchange", EXCHANGE_PDV_DLX)
                .withArgument("x-dead-letter-routing-key", QUEUE_PIX_DLQ)
                .withArgument("x-message-ttl", 300000) // 5 min TTL
                .build();
    }

    @Bean
    public Queue pixDlQueue() {
        return QueueBuilder.durable(QUEUE_PIX_DLQ).build();
    }

    // ===== BINDINGS =====
    @Bean
    public Binding syncBinding(Queue syncQueue, TopicExchange pdvExchange) {
        return BindingBuilder.bind(syncQueue).to(pdvExchange).with(ROUTING_SYNC + ".#");
    }

    @Bean
    public Binding syncDlqBinding(Queue syncDlQueue, DirectExchange pdvDlxExchange) {
        return BindingBuilder.bind(syncDlQueue).to(pdvDlxExchange).with(QUEUE_SYNC_DLQ);
    }

    @Bean
    public Binding notificacaoBinding(Queue notificacaoQueue, TopicExchange pdvExchange) {
        return BindingBuilder.bind(notificacaoQueue).to(pdvExchange).with(ROUTING_NOTIFICACAO + ".#");
    }

    @Bean
    public Binding pixBinding(Queue pixQueue, TopicExchange pdvExchange) {
        return BindingBuilder.bind(pixQueue).to(pdvExchange).with(ROUTING_PIX + ".#");
    }

    @Bean
    public Binding pixDlqBinding(Queue pixDlQueue, DirectExchange pdvDlxExchange) {
        return BindingBuilder.bind(pixDlQueue).to(pdvDlxExchange).with(QUEUE_PIX_DLQ);
    }

    // ===== CONVERTER JSON =====
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(5);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
