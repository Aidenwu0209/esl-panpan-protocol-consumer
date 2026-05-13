package com.aidenwu.esl.panpan.consumer.status;

import com.aidenwu.esl.panpan.consumer.config.PanPanProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
public class RabbitTaskStatusEventPublisher implements TaskStatusEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final PanPanProperties properties;
    private final ObjectMapper objectMapper;

    public RabbitTaskStatusEventPublisher(RabbitTemplate rabbitTemplate, PanPanProperties properties, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(TaskStatusEvent event) {
        Runnable send = () -> rabbitTemplate.send(
                properties.getRabbit().getTaskStatusExchange(),
                properties.getRabbit().getTaskStatusRoutingKey(),
                toMessage(event)
        );
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    private Message toMessage(TaskStatusEvent event) {
        try {
            return MessageBuilder
                    .withBody(objectMapper.writeValueAsString(event).getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize task status event", e);
        }
    }
}
