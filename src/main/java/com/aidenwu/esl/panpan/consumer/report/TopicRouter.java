package com.aidenwu.esl.panpan.consumer.report;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TopicRouter {

    public RoutedTopic route(String topic) {
        if (!StringUtils.hasText(topic)) {
            return unknown(topic);
        }
        String[] parts = topic.split("/");
        if (parts.length == 5
                && "esl".equals(parts[0])
                && "ap".equals(parts[1])
                && "report".equals(parts[2])) {
            String action = parts[3];
            String clientId = parts[4];
            return switch (action) {
                case "heart" -> new RoutedTopic(ReportType.AP_HEARTBEAT, topic, action, clientId, null);
                case "runinfo" -> new RoutedTopic(ReportType.AP_RUNINFO, topic, action, clientId, null);
                case "tag" -> new RoutedTopic(ReportType.ESL_REPORT, topic, action, clientId, null);
                default -> unknown(topic);
            };
        }
        if (parts.length == 4
                && "esl".equals(parts[0])
                && "ap".equals(parts[1])
                && "ack".equals(parts[2])) {
            return new RoutedTopic(ReportType.AP_ACK, topic, "ack", parts[3], null);
        }
        if (parts.length == 4
                && "esl".equals(parts[0])
                && "esl".equals(parts[1])
                && "reqkey".equals(parts[2])) {
            return new RoutedTopic(ReportType.REQ_KEY, topic, "reqkey", null, parts[3]);
        }
        return unknown(topic);
    }

    private RoutedTopic unknown(String topic) {
        return new RoutedTopic(ReportType.UNKNOWN, topic, null, null, null);
    }
}
