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
public class PanPanShopcodeBuilder extends JsonBuilderSupport implements PanPanPayloadBuilder {

    private final Clock clock;

    public PanPanShopcodeBuilder(ObjectMapper objectMapper, Clock clock) {
        super(objectMapper);
        this.clock = clock;
    }

    @Override
    public MessageType supports() {
        return MessageType.AP_BIND_SHOP;
    }

    @Override
    public MqttCommand build(PanPanCommandMessage message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("op", "shopcode");
        payload.put("shopcode", message.getShopCode());
        payload.put("timestamp", clock.millis());
        payload.put("id", message.getTaskUuid());
        payload.put("shopid", message.getShopId());
        payload.put("shopno", message.getShopNo());
        return new MqttCommand("esl/server/mgr/" + message.getApCode(), json(payload));
    }
}
