package com.aidenwu.esl.panpan.consumer.report;

public record RoutedTopic(
        ReportType reportType,
        String topic,
        String action,
        String clientId,
        String shopCode
) {
}
