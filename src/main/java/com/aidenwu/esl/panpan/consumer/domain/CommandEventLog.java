package com.aidenwu.esl.panpan.consumer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "command_event_log")
public class CommandEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_uuid", length = 64)
    private String taskUuid;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "status_from", length = 32)
    private String statusFrom;

    @Column(name = "status_to", length = 32)
    private String statusTo;

    @Column(name = "topic", length = 255)
    private String topic;

    @Column(name = "payload", columnDefinition = "longtext")
    private String payload;

    @Column(name = "message", columnDefinition = "text")
    private String message;

    @Column(name = "error", columnDefinition = "text")
    private String error;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
