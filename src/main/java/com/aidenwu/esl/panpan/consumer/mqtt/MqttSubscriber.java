package com.aidenwu.esl.panpan.consumer.mqtt;

import com.aidenwu.esl.panpan.consumer.config.PanPanProperties;
import com.aidenwu.esl.panpan.consumer.report.PanPanMqttReportConsumer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "panpan.mqtt", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MqttSubscriber implements SmartLifecycle, MqttCallbackExtended {

    private final PanPanProperties properties;
    private final PanPanMqttReportConsumer reportConsumer;
    private MqttClient client;
    private volatile boolean running;

    public MqttSubscriber(PanPanProperties properties, PanPanMqttReportConsumer reportConsumer) {
        this.properties = properties;
        this.reportConsumer = reportConsumer;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        try {
            client = new MqttClient(
                    properties.getMqtt().getBrokerUrl(),
                    properties.getMqtt().getClientIdPrefix() + "-subscriber-" + UUID.randomUUID(),
                    new MemoryPersistence()
            );
            client.setCallback(this);
            client.connect(connectOptions());
            subscribeAll();
            running = true;
            log.info("Connected MQTT subscriber to {}", properties.getMqtt().getBrokerUrl());
        } catch (MqttException e) {
            throw new IllegalStateException("Failed to start MQTT subscriber", e);
        }
    }

    @Override
    public synchronized void stop() {
        if (client != null && client.isConnected()) {
            try {
                client.disconnect();
            } catch (MqttException e) {
                log.warn("Failed to disconnect MQTT subscriber", e);
            }
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        if (reconnect) {
            try {
                subscribeAll();
            } catch (MqttException e) {
                log.warn("Failed to resubscribe MQTT topics", e);
            }
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("MQTT subscriber connection lost: {}", cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        reportConsumer.handleMessage(topic, payload);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Subscriber does not publish.
    }

    private void subscribeAll() throws MqttException {
        for (String topic : properties.getMqtt().getSubscriptions()) {
            client.subscribe(topic, properties.getMqtt().getQos());
        }
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
