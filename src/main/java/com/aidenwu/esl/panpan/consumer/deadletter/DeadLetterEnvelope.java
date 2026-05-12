package com.aidenwu.esl.panpan.consumer.deadletter;

import java.time.Instant;

public record DeadLetterEnvelope(
        String source,
        String reason,
        String payload,
        String exceptionType,
        Instant createdAt
) {
}
