package com.aidenwu.esl.panpan.consumer.report;

import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MqttReportEvent {

    private ReportType reportType;
    private String topic;
    private String action;
    private String clientId;
    private String shopCode;
    private String rawPayload;
    private Instant receivedAt;

    public MqttReportEvent(RoutedTopic route, String rawPayload, Instant receivedAt) {
        this.reportType = route.reportType();
        this.topic = route.topic();
        this.action = route.action();
        this.clientId = route.clientId();
        this.shopCode = route.shopCode();
        this.rawPayload = rawPayload;
        this.receivedAt = receivedAt;
    }
}
