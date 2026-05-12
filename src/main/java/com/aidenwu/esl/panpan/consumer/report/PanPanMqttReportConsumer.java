package com.aidenwu.esl.panpan.consumer.report;

import com.aidenwu.esl.panpan.consumer.config.PanPanProperties;
import com.aidenwu.esl.panpan.consumer.deadletter.DeadLetterPublisher;
import com.aidenwu.esl.panpan.consumer.service.EventLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PanPanMqttReportConsumer {

    private final TopicRouter topicRouter;
    private final RabbitTemplate rabbitTemplate;
    private final PanPanProperties properties;
    private final DeadLetterPublisher deadLetterPublisher;
    private final EventLogService eventLogService;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public PanPanMqttReportConsumer(
            TopicRouter topicRouter,
            RabbitTemplate rabbitTemplate,
            PanPanProperties properties,
            DeadLetterPublisher deadLetterPublisher,
            EventLogService eventLogService,
            Clock clock,
            ObjectMapper objectMapper
    ) {
        this.topicRouter = topicRouter;
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
        this.deadLetterPublisher = deadLetterPublisher;
        this.eventLogService = eventLogService;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    public void handleMessage(String topic, String payload) {
        RoutedTopic route = topicRouter.route(topic);
        if (route.reportType() == ReportType.UNKNOWN) {
            IllegalArgumentException exception = new IllegalArgumentException("Unsupported MQTT report topic: " + topic);
            deadLetterPublisher.publish("panpan.mqtt.report", payload, exception);
            eventLogService.log(null, "MQTT_REPORT_UNKNOWN", null, null, topic, payload, null, exception.getMessage());
            return;
        }

        MqttReportEvent event = new MqttReportEvent(route, payload, Instant.now(clock));
        rabbitTemplate.send(
                properties.getRabbit().getReportExchange(),
                properties.getRabbit().getReportRoutingKey(),
                toJsonMessage(event)
        );
        eventLogService.log(null, "MQTT_REPORT_RECEIVED", null, null, topic, payload, route.reportType().name(), null);
        log.debug("Routed MQTT report topic={} type={}", topic, route.reportType());
    }

    private Message toJsonMessage(MqttReportEvent event) {
        try {
            return MessageBuilder
                    .withBody(objectMapper.writeValueAsBytes(event))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize MQTT report event", e);
        }
    }
}
