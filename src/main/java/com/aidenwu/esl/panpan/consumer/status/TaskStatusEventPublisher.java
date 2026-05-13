package com.aidenwu.esl.panpan.consumer.status;

public interface TaskStatusEventPublisher {

    void publish(TaskStatusEvent event);
}
