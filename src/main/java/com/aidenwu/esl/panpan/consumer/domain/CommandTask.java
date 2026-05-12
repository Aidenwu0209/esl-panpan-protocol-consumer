package com.aidenwu.esl.panpan.consumer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "command_task")
public class CommandTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_uuid", nullable = false, unique = true, length = 64)
    private String taskUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 64)
    private MessageType messageType;

    @Column(name = "brand", length = 32)
    private String brand;

    @Column(name = "shop_code", length = 64)
    private String shopCode;

    @Column(name = "ap_code", length = 64)
    private String apCode;

    @Column(name = "tag_id", length = 64)
    private String tagId;

    @Column(name = "vendor_task_id")
    private Long vendorTaskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CommandStatus status = CommandStatus.CREATED;

    @Column(name = "allow_replay", nullable = false)
    private boolean allowReplay;

    @Column(name = "mqtt_topic", length = 255)
    private String mqttTopic;

    @Column(name = "mqtt_payload", columnDefinition = "longtext")
    private String mqttPayload;

    @Column(name = "raw_message", columnDefinition = "longtext")
    private String rawMessage;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "queued_at")
    private Instant queuedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "ap_acked_at")
    private Instant apAckedAt;

    @Column(name = "esl_reported_at")
    private Instant eslReportedAt;

    @Column(name = "success_at")
    private Instant successAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "timeout_at")
    private Instant timeoutAt;

    @Column(name = "deadline_at")
    private Instant deadlineAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
