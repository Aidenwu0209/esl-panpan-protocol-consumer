package com.aidenwu.esl.panpan.consumer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommandTaskServiceTest {

    @Mock
    private CommandTaskRepository repository;

    @Mock
    private EventLogService eventLogService;

    @Mock
    private TaskStatusEventPublisher statusEventPublisher;

    private CommandTaskService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-05-13T12:00:00Z"), ZoneOffset.UTC);
        service = new CommandTaskService(repository, eventLogService, new PanPanProperties(), clock, statusEventPublisher);
    }

    @Test
    void lateAckAfterEslReportIsLoggedButNotPublishedBackToProducer() {
        CommandTask task = task("task-1", MessageType.TAG_UPDATE, CommandStatus.ESL_REPORTED);
        when(repository.findByTaskUuidForUpdate("task-1")).thenReturn(Optional.of(task));

        service.markApAcked("task-1", "esl/ap/ack/ESLAP00000008", "{\"id\":\"task-1\",\"code\":0}");

        assertThat(task.getStatus()).isEqualTo(CommandStatus.ESL_REPORTED);
        assertThat(task.getApAckedAt()).isEqualTo(Instant.parse("2026-05-13T12:00:00Z"));
        verify(eventLogService).log(
                eq("task-1"),
                eq("AP_ACK"),
                eq(CommandStatus.ESL_REPORTED),
                eq(CommandStatus.ESL_REPORTED),
                eq("esl/ap/ack/ESLAP00000008"),
                any(),
                eq("AP ACK received"),
                eq(null)
        );
        verify(statusEventPublisher, never()).publish(any(TaskStatusEvent.class));
    }

    @Test
    void internalTagKeyReplyDoesNotPublishProducerStatusEvent() {
        CommandTask task = task("internal-task", MessageType.TAG_KEY_REPLY, CommandStatus.QUEUED);
        when(repository.findByTaskUuidForUpdate("internal-task")).thenReturn(Optional.of(task));

        service.markPublished("internal-task", new MqttCommand("esl/server/data/ZH01", "{\"command\":\"dkey\"}"));

        assertThat(task.getStatus()).isEqualTo(CommandStatus.PUBLISHED);
        verify(statusEventPublisher, never()).publish(any(TaskStatusEvent.class));
    }

    private CommandTask task(String taskUuid, MessageType messageType, CommandStatus status) {
        CommandTask task = new CommandTask();
        task.setTaskUuid(taskUuid);
        task.setMessageType(messageType);
        task.setStatus(status);
        task.setBrand("PANPAN");
        task.setApCode("ESLAP00000008");
        task.setTagId("6597069770841");
        return task;
    }
}
