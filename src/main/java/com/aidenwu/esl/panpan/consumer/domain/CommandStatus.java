package com.aidenwu.esl.panpan.consumer.domain;

public enum CommandStatus {
    CREATED,
    QUEUED,
    PUBLISHED,
    AP_ACKED,
    ESL_REPORTED,
    SUCCESS,
    FAILED,
    TIMEOUT
}
