package com.aidenwu.esl.panpan.consumer.service;

import com.aidenwu.esl.panpan.consumer.domain.CommandEventLog;
import com.aidenwu.esl.panpan.consumer.domain.CommandStatus;
import com.aidenwu.esl.panpan.consumer.repository.CommandEventLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventLogService {

    private final CommandEventLogRepository repository;

    public EventLogService(CommandEventLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(
            String taskUuid,
            String eventType,
            CommandStatus from,
            CommandStatus to,
            String topic,
            String payload,
            String message,
            String error
    ) {
        CommandEventLog log = new CommandEventLog();
        log.setTaskUuid(taskUuid);
        log.setEventType(eventType);
        log.setStatusFrom(from == null ? null : from.name());
        log.setStatusTo(to == null ? null : to.name());
        log.setTopic(topic);
        log.setPayload(payload);
        log.setMessage(message);
        log.setError(error);
        repository.save(log);
    }
}
