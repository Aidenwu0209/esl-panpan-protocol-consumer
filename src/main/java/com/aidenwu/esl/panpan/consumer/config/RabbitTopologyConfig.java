package com.aidenwu.esl.panpan.consumer.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopologyConfig {

    @Bean
    DirectExchange commandExchange(PanPanProperties properties) {
        return ExchangeBuilder.directExchange(properties.getRabbit().getCommandExchange()).durable(true).build();
    }

    @Bean
    Queue commandQueue(PanPanProperties properties) {
        return QueueBuilder.durable(properties.getRabbit().getCommandQueue())
                .withArgument("x-dead-letter-exchange", properties.getRabbit().getDeadExchange())
                .build();
    }

    @Bean
    Binding commandBinding(PanPanProperties properties, Queue commandQueue, DirectExchange commandExchange) {
        return BindingBuilder.bind(commandQueue)
                .to(commandExchange)
                .with(properties.getRabbit().getCommandRoutingKey());
    }

    @Bean
    DirectExchange reportExchange(PanPanProperties properties) {
        return ExchangeBuilder.directExchange(properties.getRabbit().getReportExchange()).durable(true).build();
    }

    @Bean
    DirectExchange taskStatusExchange(PanPanProperties properties) {
        return ExchangeBuilder.directExchange(properties.getRabbit().getTaskStatusExchange()).durable(true).build();
    }

    @Bean
    Queue reportQueue(PanPanProperties properties) {
        return QueueBuilder.durable(properties.getRabbit().getReportQueue())
                .withArgument("x-dead-letter-exchange", properties.getRabbit().getDeadExchange())
                .build();
    }

    @Bean
    Binding reportBinding(PanPanProperties properties, Queue reportQueue, DirectExchange reportExchange) {
        return BindingBuilder.bind(reportQueue)
                .to(reportExchange)
                .with(properties.getRabbit().getReportRoutingKey());
    }

    @Bean
    FanoutExchange deadExchange(PanPanProperties properties) {
        return ExchangeBuilder.fanoutExchange(properties.getRabbit().getDeadExchange()).durable(true).build();
    }

    @Bean
    Queue deadQueue(PanPanProperties properties) {
        return QueueBuilder.durable(properties.getRabbit().getDeadQueue()).build();
    }

    @Bean
    Binding deadBinding(Queue deadQueue, FanoutExchange deadExchange) {
        return BindingBuilder.bind(deadQueue).to(deadExchange);
    }
}
