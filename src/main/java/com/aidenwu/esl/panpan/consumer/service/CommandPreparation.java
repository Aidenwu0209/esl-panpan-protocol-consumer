package com.aidenwu.esl.panpan.consumer.service;

import com.aidenwu.esl.panpan.consumer.domain.CommandTask;

public record CommandPreparation(CommandTask task, boolean shouldPublish, String skipReason) {
}
