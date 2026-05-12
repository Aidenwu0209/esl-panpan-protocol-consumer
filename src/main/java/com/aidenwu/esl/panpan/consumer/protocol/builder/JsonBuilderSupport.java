package com.aidenwu.esl.panpan.consumer.protocol.builder;

import com.aidenwu.esl.panpan.consumer.protocol.ProtocolException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

abstract class JsonBuilderSupport {

    private final ObjectMapper objectMapper;

    JsonBuilderSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    String json(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new ProtocolException("Failed to serialize PanPan MQTT payload", e);
        }
    }
}
