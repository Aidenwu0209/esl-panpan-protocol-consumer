package com.aidenwu.esl.panpan.consumer.deadletter;

import com.aidenwu.esl.panpan.consumer.config.PanPanProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class DeadLetterPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final PanPanProperties properties;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public DeadLetterPublisher(
            RabbitTemplate rabbitTemplate,
            PanPanProperties properties,
            Clock clock,
            ObjectMapper objectMapper
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    public void publish(String source, String payload, Exception exception) {
        DeadLetterEnvelope envelope = new DeadLetterEnvelope(
                source,
                exception.getMessage(),
                payload,
                exception.getClass().getName(),
                Instant.now(clock)
        );
        rabbitTemplate.send(properties.getRabbit().getDeadExchange(), "", toMessage(envelope, payload));
    }

    private Message toMessage(DeadLetterEnvelope envelope, String fallbackPayload) {
        try {
            return MessageBuilder
                    .withBody(objectMapper.writeValueAsBytes(envelope))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .build();
        } catch (JsonProcessingException e) {
            return MessageBuilder
                    .withBody(fallbackPayload.getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN)
                    .build();
        }
    }
}
