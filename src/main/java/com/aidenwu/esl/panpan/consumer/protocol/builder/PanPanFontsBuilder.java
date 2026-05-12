package com.aidenwu.esl.panpan.consumer.protocol.builder;

import com.aidenwu.esl.panpan.consumer.command.PanPanCommandMessage;
import com.aidenwu.esl.panpan.consumer.domain.MessageType;
import com.aidenwu.esl.panpan.consumer.protocol.MqttCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PanPanFontsBuilder extends JsonBuilderSupport implements PanPanPayloadBuilder {

    private final Clock clock;

    public PanPanFontsBuilder(ObjectMapper objectMapper, Clock clock) {
        super(objectMapper);
        this.clock = clock;
    }

    @Override
    public MessageType supports() {
        return MessageType.FONT_PUBLISH;
    }

    @Override
    public MqttCommand build(PanPanCommandMessage message) {
        Map<String, Object> font = new LinkedHashMap<>();
        font.put("name", message.getFontName());
        font.put("md5", message.getFontMd5());
        font.put("url", message.getDownloadUrl());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("command", "fonts");
        payload.put("timestamp", clock.millis());
        payload.put("id", message.getTaskUuid());
        payload.put("fonts", List.of(font));
        return new MqttCommand("esl/server/data/" + message.getShopCode(), json(payload));
    }
}
