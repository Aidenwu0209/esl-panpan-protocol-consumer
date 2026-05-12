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
public class PanPanTmsyncBuilder extends JsonBuilderSupport implements PanPanPayloadBuilder {

    private final Clock clock;

    public PanPanTmsyncBuilder(ObjectMapper objectMapper, Clock clock) {
        super(objectMapper);
        this.clock = clock;
    }

    @Override
    public MessageType supports() {
        return MessageType.AP_TIME_SYNC;
    }

    @Override
    public MqttCommand build(PanPanCommandMessage message) {
        long now = clock.millis();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("command", "tmsync");
        payload.put("srecv", now);
        payload.put("stime", now);
        payload.put("ssend", now);
        payload.put("id", message.getTaskUuid());
        return new MqttCommand("esl/server/mgr/" + message.getApCode(), json(payload));
    }
}
