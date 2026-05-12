package com.aidenwu.esl.panpan.consumer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ap_device")
public class ApDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ap_code", nullable = false, unique = true, length = 64)
    private String apCode;

    @Column(name = "client_id", length = 128)
    private String clientId;

    @Column(name = "shop_code", length = 64)
    private String shopCode;

    @Column(name = "shop_id")
    private Long shopId;

    @Column(name = "shop_no")
    private Integer shopNo;

    @Column(name = "online", nullable = false)
    private boolean online;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Column(name = "heartbeat_index")
    private Long heartbeatIndex;

    @Column(name = "cpu_usage")
    private Double cpuUsage;

    @Column(name = "memory_usage")
    private Double memoryUsage;

    @Column(name = "disk_usage")
    private Double diskUsage;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "mac_address", length = 64)
    private String macAddress;

    @Column(name = "firmware_version", length = 128)
    private String firmwareVersion;

    @Column(name = "app_version", length = 128)
    private String appVersion;

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
