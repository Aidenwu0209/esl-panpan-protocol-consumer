package com.aidenwu.esl.panpan.consumer.report;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TopicRouterTest {

    private final TopicRouter router = new TopicRouter();

    @Test
    void routesApReportTopics() {
        RoutedTopic heartbeat = router.route("esl/ap/report/heart/ESLAP00000008");
        assertThat(heartbeat.reportType()).isEqualTo(ReportType.AP_HEARTBEAT);
        assertThat(heartbeat.clientId()).isEqualTo("ESLAP00000008");

        RoutedTopic runinfo = router.route("esl/ap/report/runinfo/ESLAP00000008");
        assertThat(runinfo.reportType()).isEqualTo(ReportType.AP_RUNINFO);

        RoutedTopic tag = router.route("esl/ap/report/tag/ESLAP00000008");
        assertThat(tag.reportType()).isEqualTo(ReportType.ESL_REPORT);
    }

    @Test
    void routesAckAndReqkeyTopics() {
        RoutedTopic ack = router.route("esl/ap/ack/ESLAP00000008");
        assertThat(ack.reportType()).isEqualTo(ReportType.AP_ACK);
        assertThat(ack.clientId()).isEqualTo("ESLAP00000008");

        RoutedTopic reqkey = router.route("esl/esl/reqkey/ZH01");
        assertThat(reqkey.reportType()).isEqualTo(ReportType.REQ_KEY);
        assertThat(reqkey.shopCode()).isEqualTo("ZH01");
    }

    @Test
    void returnsUnknownForUnsupportedTopic() {
        assertThat(router.route("bad/topic").reportType()).isEqualTo(ReportType.UNKNOWN);
    }
}
