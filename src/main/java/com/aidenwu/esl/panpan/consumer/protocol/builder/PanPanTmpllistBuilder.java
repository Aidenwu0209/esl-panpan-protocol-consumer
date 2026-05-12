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
public class PanPanTmpllistBuilder extends JsonBuilderSupport implements PanPanPayloadBuilder {

    private final Clock clock;

    public PanPanTmpllistBuilder(ObjectMapper objectMapper, Clock clock) {
        super(objectMapper);
        this.clock = clock;
    }

    @Override
    public MessageType supports() {
        return MessageType.TEMPLATE_PUBLISH;
    }

    @Override
    public MqttCommand build(PanPanCommandMessage message) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("name", message.getTemplateName());
        template.put("screen", message.getScreenCode());
        template.put("filename", message.getFileName());
        template.put("md5", message.getFileMd5());
        template.put("url", message.getDownloadUrl());
        template.put("tenantid", message.getTenantId());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("command", "tmpllist");
        payload.put("timestamp", clock.millis());
        payload.put("id", message.getTaskUuid());
        payload.put("tmpllist", List.of(template));
        return new MqttCommand("esl/server/data/" + message.getShopCode(), json(payload));
    }
}
