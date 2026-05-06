package com.hotpulse.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String CRAWL_EXCHANGE = "hotpulse.crawl.exchange";
    public static final String ANALYZE_EXCHANGE = "hotpulse.analyze.exchange";
    public static final String CRAWL_QUEUE = "hotpulse.crawl.queue";
    public static final String ANALYZE_QUEUE = "hotpulse.analyze.queue";
    public static final String CRAWL_ROUTING_KEY = "crawl.task";
    public static final String ANALYZE_ROUTING_KEY = "analyze.task";

    @Bean
    public DirectExchange crawlExchange() {
        return new DirectExchange(CRAWL_EXCHANGE);
    }

    @Bean
    public DirectExchange analyzeExchange() {
        return new DirectExchange(ANALYZE_EXCHANGE);
    }

    @Bean
    public Queue crawlQueue() {
        return QueueBuilder.durable(CRAWL_QUEUE).build();
    }

    @Bean
    public Queue analyzeQueue() {
        return QueueBuilder.durable(ANALYZE_QUEUE).build();
    }

    @Bean
    public Binding crawlBinding(Queue crawlQueue, DirectExchange crawlExchange) {
        return BindingBuilder.bind(crawlQueue).to(crawlExchange).with(CRAWL_ROUTING_KEY);
    }

    @Bean
    public Binding analyzeBinding(Queue analyzeQueue, DirectExchange analyzeExchange) {
        return BindingBuilder.bind(analyzeQueue).to(analyzeExchange).with(ANALYZE_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
