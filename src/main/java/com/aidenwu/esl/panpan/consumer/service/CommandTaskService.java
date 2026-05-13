package com.aidenwu.esl.panpan.consumer.service;

import com.aidenwu.esl.panpan.consumer.command.PanPanCommandMessage;
import com.aidenwu.esl.panpan.consumer.config.PanPanProperties;
import com.aidenwu.esl.panpan.consumer.domain.CommandStatus;
import com.aidenwu.esl.panpan.consumer.domain.CommandTask;
import com.aidenwu.esl.panpan.consumer.domain.MessageType;
import com.aidenwu.esl.panpan.consumer.protocol.MqttCommand;
import com.aidenwu.esl.panpan.consumer.repository.CommandTaskRepository;
import com.aidenwu.esl.panpan.consumer.status.TaskStatusEvent;
import com.aidenwu.esl.panpan.consumer.status.TaskStatusEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommandTaskService {

    private static final EnumSet<CommandStatus> PUBLISHED_OR_AFTER = EnumSet.of(
            CommandStatus.PUBLISHED,
            CommandStatus.AP_ACKED,
            CommandStatus.ESL_REPORTED,
            CommandStatus.SUCCESS,
            CommandStatus.FAILED,
            CommandStatus.TIMEOUT
    );

    private static final List<CommandStatus> ACTIVE_BY_TAG = List.of(
            CommandStatus.PUBLISHED,
            CommandStatus.AP_ACKED
    );

    private final CommandTaskRepository repository;
    private final EventLogService eventLogService;
    private final PanPanProperties properties;
    private final Clock clock;
    private final TaskStatusEventPublisher statusEventPublisher;

    public CommandTaskService(
            CommandTaskRepository repository,
            EventLogService eventLogService,
            PanPanProperties properties,
            Clock clock,
            TaskStatusEventPublisher statusEventPublisher
    ) {
        this.repository = repository;
        this.eventLogService = eventLogService;
        this.properties = properties;
        this.clock = clock;
        this.statusEventPublisher = statusEventPublisher;
    }

    @Transactional
    public CommandPreparation prepareForPublish(PanPanCommandMessage message, String rawMessage) {
        Optional<CommandTask> existing = repository.findByTaskUuidForUpdate(message.getTaskUuid());
        if (existing.isPresent()) {
            CommandTask task = existing.get();
            copyCommandFields(task, message, rawMessage);
            if (PUBLISHED_OR_AFTER.contains(task.getStatus()) && !message.allowReplayValue()) {
                eventLogService.log(
                        task.getTaskUuid(),
                        "IDEMPOTENT_SKIP",
                        task.getStatus(),
                        task.getStatus(),
                        task.getMqttTopic(),
                        rawMessage,
                        "Task already published or completed; MQTT replay skipped",
                        null
                );
                return new CommandPreparation(task, false, "already-published");
            }
            CommandStatus from = task.getStatus();
            task.setStatus(CommandStatus.QUEUED);
            task.setQueuedAt(Instant.now(clock));
            task.setDeadlineAt(Instant.now(clock).plus(properties.getTimeout().getCommandTtl()));
            repository.save(task);
            eventLogService.log(task.getTaskUuid(), "QUEUED", from, task.getStatus(), null, rawMessage, null, null);
            return new CommandPreparation(task, true, null);
        }

        CommandTask task = new CommandTask();
        task.setTaskUuid(message.getTaskUuid());
        copyCommandFields(task, message, rawMessage);
        task.setStatus(CommandStatus.QUEUED);
        task.setQueuedAt(Instant.now(clock));
        task.setDeadlineAt(Instant.now(clock).plus(properties.getTimeout().getCommandTtl()));
        repository.save(task);
        eventLogService.log(task.getTaskUuid(), "QUEUED", CommandStatus.CREATED, task.getStatus(), null, rawMessage, null, null);
        return new CommandPreparation(task, true, null);
    }

    private void copyCommandFields(CommandTask task, PanPanCommandMessage message, String rawMessage) {
        task.setMessageType(message.getMessageType());
        task.setBrand(message.getBrand());
        task.setShopCode(message.getShopCode());
        task.setApCode(message.getApCode());
        task.setTagId(message.getTagId());
        task.setVendorTaskId(message.getVendorTaskId());
        task.setAllowReplay(message.allowReplayValue());
        task.setRawMessage(rawMessage);
    }

    @Transactional
    public void markPublished(String taskUuid, MqttCommand mqttCommand) {
        CommandTask task = repository.findByTaskUuidForUpdate(taskUuid)
                .orElseThrow(() -> new IllegalArgumentException("command_task not found: " + taskUuid));
        CommandStatus from = task.getStatus();
        task.setStatus(CommandStatus.PUBLISHED);
        task.setMqttTopic(mqttCommand.topic());
        task.setMqttPayload(mqttCommand.payload());
        Instant now = Instant.now(clock);
        task.setPublishedAt(now);
        task.setErrorMessage(null);
        repository.save(task);
        eventLogService.log(taskUuid, "MQTT_PUBLISHED", from, task.getStatus(), mqttCommand.topic(), mqttCommand.payload(), null, null);
        publishStatus(task, CommandStatus.PUBLISHED, "MQTT_PUBLISHED", now, mqttCommand.topic(), "MQTT publish succeeded", null);
    }

    @Transactional
    public void markApAcked(String taskUuid, String topic, String payload) {
        repository.findByTaskUuidForUpdate(taskUuid).ifPresent(task -> {
            CommandStatus from = task.getStatus();
            boolean publishAckStatus = shouldPublishAckStatus(task.getStatus());
            Instant now = Instant.now(clock);
            if (task.getApAckedAt() == null) {
                task.setApAckedAt(now);
            }
            if (publishAckStatus) {
                task.setStatus(CommandStatus.AP_ACKED);
            }
            repository.save(task);
            eventLogService.log(taskUuid, "AP_ACK", from, task.getStatus(), topic, payload, "AP ACK received", null);
            if (publishAckStatus) {
                publishStatus(task, task.getStatus(), "AP_ACK", now, topic, "AP ACK received", null);
            }
        });
    }

    @Transactional
    public void markEslReported(String taskUuid, String topic, String payload) {
        repository.findByTaskUuidForUpdate(taskUuid).ifPresent(task -> {
            CommandStatus from = task.getStatus();
            Instant now = Instant.now(clock);
            if (task.getEslReportedAt() == null) {
                task.setEslReportedAt(now);
            }
            boolean publishReportStatus = !isTerminal(task.getStatus());
            if (publishReportStatus) {
                task.setStatus(CommandStatus.ESL_REPORTED);
            }
            repository.save(task);
            eventLogService.log(taskUuid, "ESL_REPORTED", from, task.getStatus(), topic, payload, null, null);
            if (publishReportStatus) {
                publishStatus(task, task.getStatus(), "ESL_REPORTED", now, topic, null, null);
            }
        });
    }

    @Transactional
    public void markLatestTagTaskEslReported(String tagId, String topic, String payload) {
        repository.findTop10ByTagIdAndStatusInOrderByCreatedAtDesc(tagId, ACTIVE_BY_TAG)
                .stream()
                .findFirst()
                .ifPresent(task -> markEslReported(task.getTaskUuid(), topic, payload));
    }

    @Transactional
    public void markFailed(String taskUuid, String error) {
        repository.findByTaskUuidForUpdate(taskUuid).ifPresent(task -> {
            CommandStatus from = task.getStatus();
            Instant now = Instant.now(clock);
            task.setStatus(CommandStatus.FAILED);
            task.setFailedAt(now);
            task.setErrorMessage(error);
            repository.save(task);
            eventLogService.log(taskUuid, "FAILED", from, task.getStatus(), task.getMqttTopic(), task.getRawMessage(), null, error);
            publishStatus(task, CommandStatus.FAILED, "FAILED", now, task.getMqttTopic(), null, error);
        });
    }

    @Transactional
    public int markTimedOut() {
        Instant now = Instant.now(clock);
        List<CommandTask> expired = repository.findTop200ByStatusInAndDeadlineAtBefore(
                List.of(CommandStatus.QUEUED, CommandStatus.PUBLISHED, CommandStatus.AP_ACKED),
                now
        );
        expired.forEach(task -> {
            CommandStatus from = task.getStatus();
            task.setStatus(CommandStatus.TIMEOUT);
            task.setTimeoutAt(now);
            task.setErrorMessage("Command timed out before terminal device report");
            repository.save(task);
            eventLogService.log(task.getTaskUuid(), "TIMEOUT", from, task.getStatus(), task.getMqttTopic(), task.getMqttPayload(), null, task.getErrorMessage());
            publishStatus(task, CommandStatus.TIMEOUT, "TIMEOUT", now, task.getMqttTopic(), null, task.getErrorMessage());
        });
        return expired.size();
    }

    @Transactional(readOnly = true)
    public Optional<CommandTask> findByTaskUuid(String taskUuid) {
        return repository.findByTaskUuid(taskUuid);
    }

    private boolean isTerminal(CommandStatus status) {
        return status == CommandStatus.SUCCESS || status == CommandStatus.FAILED || status == CommandStatus.TIMEOUT;
    }

    private boolean shouldPublishAckStatus(CommandStatus status) {
        return status != CommandStatus.ESL_REPORTED && !isTerminal(status);
    }

    private void publishStatus(
            CommandTask task,
            CommandStatus status,
            String stage,
            Instant occurredAt,
            String topic,
            String message,
            String errorMessage
    ) {
        if (task.getMessageType() == MessageType.TAG_KEY_REPLY) {
            return;
        }
        statusEventPublisher.publish(TaskStatusEvent.from(task, status, stage, occurredAt, topic, message, errorMessage));
    }
}
