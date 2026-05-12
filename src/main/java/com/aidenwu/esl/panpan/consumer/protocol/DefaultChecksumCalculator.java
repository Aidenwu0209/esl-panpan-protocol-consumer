package com.aidenwu.esl.panpan.consumer.protocol;

import com.aidenwu.esl.panpan.consumer.command.PanPanCommandMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DefaultChecksumCalculator implements ChecksumCalculator {

    private final ObjectMapper objectMapper;

    public DefaultChecksumCalculator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String calculate(PanPanCommandMessage message, Map<String, Object> value) {
        try {
            String material = message.getTaskUuid() + "|" + message.getTagId() + "|"
                    + objectMapper.writeValueAsString(value);
            return sha256(material).substring(0, 32);
        } catch (JsonProcessingException e) {
            throw new ProtocolException("Failed to serialize wtag value for checksum", e);
        }
    }

    private String sha256(String material) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(material.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
