package com.aidenwu.esl.panpan.consumer.protocol;

public record MqttCommand(String topic, String payload) {
}
