package com.aidenwu.esl.panpan.consumer.mqtt;

import com.aidenwu.esl.panpan.consumer.config.PanPanProperties;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "panpan.mqtt", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PahoMqttPublisher implements MqttPublisher {

    private final PanPanProperties properties;
    private MqttClient client;

    public PahoMqttPublisher(PanPanProperties properties) {
        this.properties = properties;
    }

    @Override
    public synchronized void publish(String topic, String payload) {
        try {
            ensureConnected();
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(properties.getMqtt().getQos());
            message.setRetained(false);
            client.publish(topic, message);
        } catch (MqttException e) {
            throw new MqttPublishException("Failed to publish MQTT message to " + topic, e);
        }
    }

    private void ensureConnected() throws MqttException {
        if (client != null && client.isConnected()) {
            return;
        }
        client = new MqttClient(
                properties.getMqtt().getBrokerUrl(),
                properties.getMqtt().getClientIdPrefix() + "-publisher-" + UUID.randomUUID(),
                new MemoryPersistence()
        );
        client.connect(connectOptions());
        log.info("Connected MQTT publisher to {}", properties.getMqtt().getBrokerUrl());
    }

    private MqttConnectOptions connectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        if (StringUtils.hasText(properties.getMqtt().getUsername())) {
            options.setUserName(properties.getMqtt().getUsername());
        }
        if (StringUtils.hasText(properties.getMqtt().getPassword())) {
            options.setPassword(properties.getMqtt().getPassword().toCharArray());
        }
        return options;
    }
}
