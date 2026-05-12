package com.aidenwu.esl.panpan.consumer.device;

import com.aidenwu.esl.panpan.consumer.config.PanPanProperties;
import com.aidenwu.esl.panpan.consumer.domain.TagKey;
import com.aidenwu.esl.panpan.consumer.repository.TagKeyRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagKeyService {

    private final TagKeyRepository repository;
    private final PanPanProperties properties;

    public TagKeyService(TagKeyRepository repository, PanPanProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Transactional
    public TagKey getOrCreateForReqkey(String tagId, String shopCode, String apCode) {
        return repository.findByTagId(tagId).orElseGet(() -> {
            if (!properties.getSecurity().isAutoCreateMissingTagKey()) {
                throw new IllegalStateException("No tag_key found for tagId=" + tagId);
            }
            TagKey tagKey = new TagKey();
            tagKey.setTagId(tagId);
            tagKey.setShopCode(shopCode);
            tagKey.setApCode(apCode);
            tagKey.setSk("sk_" + sha256("sk|" + tagId + "|" + properties.getSecurity().getDefaultKeySecret()).substring(0, 32));
            tagKey.setTk("tk_" + sha256("tk|" + tagId + "|" + properties.getSecurity().getDefaultKeySecret()).substring(0, 32));
            return repository.save(tagKey);
        });
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
