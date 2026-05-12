package com.aidenwu.esl.panpan.consumer.report;

import com.aidenwu.esl.panpan.consumer.deadletter.DeadLetterPublisher;
import com.aidenwu.esl.panpan.consumer.device.DeviceReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CommandStatusConsumer {

    private final ObjectMapper objectMapper;
    private final DeviceReportService deviceReportService;
    private final DeadLetterPublisher deadLetterPublisher;

    public CommandStatusConsumer(
            ObjectMapper objectMapper,
            DeviceReportService deviceReportService,
            DeadLetterPublisher deadLetterPublisher
    ) {
        this.objectMapper = objectMapper;
        this.deviceReportService = deviceReportService;
        this.deadLetterPublisher = deadLetterPublisher;
    }

    @RabbitListener(queues = "${panpan.rabbit.report-queue}", ackMode = "MANUAL")
    public void consume(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String raw = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            MqttReportEvent event = objectMapper.readValue(raw, MqttReportEvent.class);
            switch (event.getReportType()) {
                case AP_HEARTBEAT -> deviceReportService.handleHeartbeat(event);
                case AP_RUNINFO -> deviceReportService.handleRuninfo(event);
                case ESL_REPORT -> deviceReportService.handleEslReport(event);
                case AP_ACK -> deviceReportService.handleAck(event);
                case REQ_KEY -> deviceReportService.handleReqkey(event);
                case UNKNOWN -> throw new IllegalArgumentException("Unsupported report type UNKNOWN");
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.warn("Failed to consume report event: {}", e.getMessage(), e);
            deadLetterPublisher.publish("panpan.report.queue", raw, e);
            channel.basicAck(deliveryTag, false);
        }
    }
}
