package com.aidenwu.esl.panpan.consumer.protocol;

import com.aidenwu.esl.panpan.consumer.command.PanPanCommandMessage;
import com.aidenwu.esl.panpan.consumer.config.PanPanProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Component;

@Component
public class DefaultTokenProvider implements TokenProvider {

    private final PanPanProperties properties;

    public DefaultTokenProvider(PanPanProperties properties) {
        this.properties = properties;
    }

    @Override
    public String issueToken(PanPanCommandMessage message) {
        String material = message.getTaskUuid() + "|" + message.getTagId() + "|"
                + message.getShopCode() + "|" + properties.getProtocol().getDefaultTokenSecret();
        return sha256(material).substring(0, 32);
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
