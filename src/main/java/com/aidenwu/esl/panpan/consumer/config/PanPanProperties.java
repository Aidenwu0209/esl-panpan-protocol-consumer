package com.aidenwu.esl.panpan.consumer.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "panpan")
public class PanPanProperties {

    private Rabbit rabbit = new Rabbit();
    private Mqtt mqtt = new Mqtt();
    private Protocol protocol = new Protocol();
    private Security security = new Security();
    private Timeout timeout = new Timeout();

    @Getter
    @Setter
    public static class Rabbit {
        private String commandExchange = "esl.command.exchange";
        private String commandQueue = "panpan.command.queue";
        private String commandRoutingKey = "panpan.command";
        private String reportExchange = "esl.report.exchange";
        private String reportQueue = "panpan.report.queue";
        private String reportRoutingKey = "panpan.report";
        private String deadExchange = "esl.dead.exchange";
        private String deadQueue = "panpan.dead.queue";
    }

    @Getter
    @Setter
    public static class Mqtt {
        private boolean enabled = true;
        private String brokerUrl = "tcp://localhost:1883";
        private String clientIdPrefix = "panpan-protocol-consumer";
        private String username;
        private String password;
        private int qos = 1;
        private String[] subscriptions = {
                "esl/ap/report/+/+",
                "esl/ap/ack/+",
                "esl/esl/reqkey/+"
        };
    }

    @Getter
    @Setter
    public static class Protocol {
        private String defaultTokenSecret = "panpan-mvp-token-secret";
    }

    @Getter
    @Setter
    public static class Security {
        private boolean autoCreateMissingTagKey = true;
        private String defaultKeySecret = "panpan-mvp-key-secret";
    }

    @Getter
    @Setter
    public static class Timeout {
        private boolean enabled = true;
        private Duration commandTtl = Duration.ofMinutes(5);
        private Duration scanInterval = Duration.ofSeconds(30);
    }
}
