package com.aidenwu.esl.panpan.consumer.protocol;

import com.aidenwu.esl.panpan.consumer.command.PanPanCommandMessage;
import com.aidenwu.esl.panpan.consumer.domain.MessageType;
import com.aidenwu.esl.panpan.consumer.protocol.builder.PanPanPayloadBuilder;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PanPanAdapter {

    private final CommandValidator validator;
    private final Map<MessageType, PanPanPayloadBuilder> builders = new EnumMap<>(MessageType.class);

    public PanPanAdapter(CommandValidator validator, List<PanPanPayloadBuilder> builders) {
        this.validator = validator;
        builders.forEach(builder -> this.builders.put(builder.supports(), builder));
    }

    public MqttCommand adapt(PanPanCommandMessage message) {
        validator.validate(message);
        PanPanPayloadBuilder builder = builders.get(message.getMessageType());
        if (builder == null) {
            throw new ProtocolException("Unsupported messageType: " + message.getMessageType());
        }
        return builder.build(message);
    }
}
