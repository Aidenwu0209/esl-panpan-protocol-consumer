package com.aidenwu.esl.panpan.consumer.command;

import com.aidenwu.esl.panpan.consumer.config.PanPanProperties;
import com.aidenwu.esl.panpan.consumer.domain.MessageType;
import com.aidenwu.esl.panpan.consumer.domain.TagKey;
import com.aidenwu.esl.panpan.consumer.service.CommandTaskService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class CommandRabbitProducer {

    private final RabbitTemplate rabbitTemplate;
    private final PanPanProperties properties;
    private final ObjectMapper objectMapper;
    private final CommandTaskService commandTaskService;

    public CommandRabbitProducer(
            RabbitTemplate rabbitTemplate,
            PanPanProperties properties,
            ObjectMapper objectMapper,
            CommandTaskService commandTaskService
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.commandTaskService = commandTaskService;
    }

    public String enqueueTagKeyReply(String shopCode, String apCode, String tagId, TagKey tagKey) {
        PanPanCommandMessage command = new PanPanCommandMessage();
        command.setMessageType(MessageType.TAG_KEY_REPLY);
        command.setBrand("PANPAN");
        command.setShopCode(shopCode);
        command.setApCode(apCode);
        command.setTagId(tagId);
        command.setSk(tagKey.getSk());
        command.setTk(tagKey.getTk());
        command.setTaskUuid(UUID.randomUUID().toString());

        String raw = write(command);
        commandTaskService.prepareForPublish(command, raw);
        Message message = MessageBuilder
                .withBody(raw.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .build();
        sendAfterCommit(message);
        return command.getTaskUuid();
    }

    private void sendAfterCommit(Message message) {
        Runnable send = () -> rabbitTemplate.send(
                properties.getRabbit().getCommandExchange(),
                properties.getRabbit().getCommandRoutingKey(),
                message
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

    private String write(PanPanCommandMessage command) {
        try {
            return objectMapper.writeValueAsString(command);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize internal TAG_KEY_REPLY command", e);
        }
    }
}
