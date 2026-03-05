package com.fooddelivery.deliveryservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class RabbitConfig {

    // Consuming from Order Service
    public static final String ORDER_EXCHANGE        = "order.exchange";
    public static final String ORDER_PLACED_QUEUE    = "order.placed.queue";
    public static final String ORDER_PLACED_KEY      = "order.placed";
    public static final String ORDER_CANCELLED_QUEUE = "order.cancelled.queue";
    public static final String ORDER_CANCELLED_KEY   = "order.cancelled";

    // Publishing delivery events
    public static final String DELIVERY_EXCHANGE     = "delivery.exchange";
    public static final String DELIVERY_STATUS_QUEUE = "delivery.status.queue";
    public static final String DELIVERY_STATUS_KEY   = "delivery.status.updated";

    // Dead Letter
    public static final String DLQ_EXCHANGE = "order.dlq.exchange";
    public static final String DLQ_QUEUE    = "order.dead.letter.queue";
    public static final String DLQ_KEY      = "order.dead";

    @Bean public TopicExchange orderExchange()    { return new TopicExchange(ORDER_EXCHANGE); }
    @Bean public TopicExchange deliveryExchange() { return new TopicExchange(DELIVERY_EXCHANGE); }
    @Bean public TopicExchange dlqExchange()      { return new TopicExchange(DLQ_EXCHANGE); }

    @Bean
    public Queue orderPlacedQueue() {
        return QueueBuilder.durable(ORDER_PLACED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_KEY)
                .build();
    }

    @Bean
    public Queue orderCancelledQueue() {
        return QueueBuilder.durable(ORDER_CANCELLED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_KEY)
                .build();
    }

    @Bean
    public Queue deliveryStatusQueue() {
        return new Queue(DELIVERY_STATUS_QUEUE, true);
    }

    @Bean
    public Queue deadLetterQueue() { return new Queue(DLQ_QUEUE, true); }

    @Bean
    public Binding orderPlacedBinding() {
        return BindingBuilder.bind(orderPlacedQueue()).to(orderExchange()).with(ORDER_PLACED_KEY);
    }

    @Bean
    public Binding orderCancelledBinding() {
        return BindingBuilder.bind(orderCancelledQueue()).to(orderExchange()).with(ORDER_CANCELLED_KEY);
    }

    @Bean
    public Binding deliveryStatusBinding() {
        return BindingBuilder.bind(deliveryStatusQueue()).to(deliveryExchange()).with(DELIVERY_STATUS_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}