package com.aidenwu.esl.panpan.consumer.service;

import com.aidenwu.esl.panpan.consumer.command.PanPanCommandMessage;
import com.aidenwu.esl.panpan.consumer.config.PanPanProperties;
import com.aidenwu.esl.panpan.consumer.domain.CommandStatus;
import com.aidenwu.esl.panpan.consumer.domain.CommandTask;
import com.aidenwu.esl.panpan.consumer.protocol.MqttCommand;
import com.aidenwu.esl.panpan.consumer.repository.CommandTaskRepository;
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

    public CommandTaskService(
            CommandTaskRepository repository,
            EventLogService eventLogService,
            PanPanProperties properties,
            Clock clock
    ) {
        this.repository = repository;
        this.eventLogService = eventLogService;
        this.properties = properties;
        this.clock = clock;
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
        task.setPublishedAt(Instant.now(clock));
        task.setErrorMessage(null);
        repository.save(task);
        eventLogService.log(taskUuid, "MQTT_PUBLISHED", from, task.getStatus(), mqttCommand.topic(), mqttCommand.payload(), null, null);
    }

    @Transactional
    public void markApAcked(String taskUuid, String topic, String payload) {
        repository.findByTaskUuidForUpdate(taskUuid).ifPresent(task -> {
            CommandStatus from = task.getStatus();
            if (task.getApAckedAt() == null) {
                task.setApAckedAt(Instant.now(clock));
            }
            if (!isTerminal(task.getStatus()) && task.getStatus() != CommandStatus.ESL_REPORTED) {
                task.setStatus(CommandStatus.AP_ACKED);
            }
            repository.save(task);
            eventLogService.log(taskUuid, "AP_ACK", from, task.getStatus(), topic, payload, "AP ACK received", null);
        });
    }

    @Transactional
    public void markEslReported(String taskUuid, String topic, String payload) {
        repository.findByTaskUuidForUpdate(taskUuid).ifPresent(task -> {
            CommandStatus from = task.getStatus();
            if (task.getEslReportedAt() == null) {
                task.setEslReportedAt(Instant.now(clock));
            }
            if (!isTerminal(task.getStatus())) {
                task.setStatus(CommandStatus.ESL_REPORTED);
            }
            repository.save(task);
            eventLogService.log(taskUuid, "ESL_REPORTED", from, task.getStatus(), topic, payload, null, null);
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
            task.setStatus(CommandStatus.FAILED);
            task.setFailedAt(Instant.now(clock));
            task.setErrorMessage(error);
            repository.save(task);
            eventLogService.log(taskUuid, "FAILED", from, task.getStatus(), task.getMqttTopic(), task.getRawMessage(), null, error);
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
}
