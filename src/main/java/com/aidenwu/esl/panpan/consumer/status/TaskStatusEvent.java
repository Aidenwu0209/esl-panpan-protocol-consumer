package com.aidenwu.esl.panpan.consumer.status;

import com.aidenwu.esl.panpan.consumer.domain.CommandStatus;
import com.aidenwu.esl.panpan.consumer.domain.CommandTask;
import com.aidenwu.esl.panpan.consumer.domain.MessageType;
import java.time.Instant;
import java.util.UUID;

public record TaskStatusEvent(
        UUID eventId,
        String taskUuid,
        MessageType messageType,
        CommandStatus status,
        String stage,
        String source,
        Instant occurredAt,
        String apCode,
        String tagId,
        Long vendorTaskId,
        String topic,
        String message,
        String errorMessage
) {

    public static TaskStatusEvent from(
            CommandTask task,
            CommandStatus status,
            String stage,
            Instant occurredAt,
            String topic,
            String message,
            String errorMessage
    ) {
        return new TaskStatusEvent(
                UUID.randomUUID(),
                task.getTaskUuid(),
                task.getMessageType(),
                status,
                stage,
                "protocol-consumer",
                occurredAt,
                task.getApCode(),
                task.getTagId(),
                task.getVendorTaskId(),
                topic,
                message,
                errorMessage
        );
    }
}
