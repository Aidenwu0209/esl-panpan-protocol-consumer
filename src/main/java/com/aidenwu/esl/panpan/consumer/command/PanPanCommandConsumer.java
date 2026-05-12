package com.aidenwu.esl.panpan.consumer.command;

import com.aidenwu.esl.panpan.consumer.deadletter.DeadLetterPublisher;
import com.aidenwu.esl.panpan.consumer.mqtt.MqttPublisher;
import com.aidenwu.esl.panpan.consumer.protocol.MqttCommand;
import com.aidenwu.esl.panpan.consumer.protocol.PanPanAdapter;
import com.aidenwu.esl.panpan.consumer.service.CommandPreparation;
import com.aidenwu.esl.panpan.consumer.service.CommandTaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PanPanCommandConsumer {

    private final ObjectMapper objectMapper;
    private final PanPanAdapter adapter;
    private final MqttPublisher mqttPublisher;
    private final CommandTaskService commandTaskService;
    private final DeadLetterPublisher deadLetterPublisher;

    public PanPanCommandConsumer(
            ObjectMapper objectMapper,
            PanPanAdapter adapter,
            MqttPublisher mqttPublisher,
            CommandTaskService commandTaskService,
            DeadLetterPublisher deadLetterPublisher
    ) {
        this.objectMapper = objectMapper;
        this.adapter = adapter;
        this.mqttPublisher = mqttPublisher;
        this.commandTaskService = commandTaskService;
        this.deadLetterPublisher = deadLetterPublisher;
    }

    @RabbitListener(queues = "${panpan.rabbit.command-queue}", ackMode = "MANUAL")
    public void consume(Message amqpMessage, Channel channel) throws Exception {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        String raw = new String(amqpMessage.getBody(), StandardCharsets.UTF_8);
        PanPanCommandMessage command = null;
        try {
            command = objectMapper.readValue(raw, PanPanCommandMessage.class);
            CommandPreparation preparation = commandTaskService.prepareForPublish(command, raw);
            if (!preparation.shouldPublish()) {
                log.info("Skip duplicate taskUuid={} reason={}", command.getTaskUuid(), preparation.skipReason());
                channel.basicAck(deliveryTag, false);
                return;
            }

            MqttCommand mqttCommand = adapter.adapt(command);
            mqttPublisher.publish(mqttCommand.topic(), mqttCommand.payload());
            commandTaskService.markPublished(command.getTaskUuid(), mqttCommand);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.warn("Failed to consume command message: {}", e.getMessage(), e);
            if (command != null && command.getTaskUuid() != null) {
                commandTaskService.markFailed(command.getTaskUuid(), e.getMessage());
            }
            deadLetterPublisher.publish("panpan.command.queue", raw, e);
            channel.basicAck(deliveryTag, false);
        }
    }
}
