package com.aidenwu.esl.panpan.consumer.protocol.builder;

import com.aidenwu.esl.panpan.consumer.command.PanPanCommandMessage;
import com.aidenwu.esl.panpan.consumer.domain.MessageType;
import com.aidenwu.esl.panpan.consumer.protocol.MqttCommand;

public interface PanPanPayloadBuilder {

    MessageType supports();

    MqttCommand build(PanPanCommandMessage message);
}
