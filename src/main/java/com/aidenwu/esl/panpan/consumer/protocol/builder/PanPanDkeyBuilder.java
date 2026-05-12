package com.aidenwu.esl.panpan.consumer.protocol.builder;

import com.aidenwu.esl.panpan.consumer.command.PanPanCommandMessage;
import com.aidenwu.esl.panpan.consumer.domain.MessageType;
import com.aidenwu.esl.panpan.consumer.protocol.MqttCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PanPanDkeyBuilder extends JsonBuilderSupport implements PanPanPayloadBuilder {

    private final Clock clock;

    public PanPanDkeyBuilder(ObjectMapper objectMapper, Clock clock) {
        super(objectMapper);
        this.clock = clock;
    }

    @Override
    public MessageType supports() {
        return MessageType.TAG_KEY_REPLY;
    }

    @Override
    public MqttCommand build(PanPanCommandMessage message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("command", "dkey");
        payload.put("timestamp", clock.millis());
        payload.put("id", message.getTaskUuid());
        payload.put("tag", message.getTagId());
        payload.put("sk", message.getSk());
        payload.put("tk", message.getTk());
        return new MqttCommand("esl/server/data/" + message.getShopCode(), json(payload));
    }
}
