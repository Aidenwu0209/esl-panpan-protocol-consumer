package com.aidenwu.esl.panpan.consumer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "esl_tag")
public class EslTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tag_id", nullable = false, unique = true, length = 64)
    private String tagId;

    @Column(name = "ap_code", length = 64)
    private String apCode;

    @Column(name = "shop_code", length = 64)
    private String shopCode;

    @Column(name = "battery_soc")
    private Integer batterySoc;

    @Column(name = "ssirs")
    private Integer ssirs;

    @Column(name = "temperature", precision = 10, scale = 2)
    private BigDecimal temperature;

    @Column(name = "stat_raw", length = 64)
    private String statRaw;

    @Column(name = "last_report_at")
    private Instant lastReportAt;

    @Column(name = "last_payload", columnDefinition = "longtext")
    private String lastPayload;

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
