package com.aidenwu.esl.panpan.consumer.mqtt;

public interface MqttPublisher {

    void publish(String topic, String payload);
}
