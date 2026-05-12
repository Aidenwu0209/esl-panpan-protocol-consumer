package com.aidenwu.esl.panpan.consumer.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.aidenwu.esl.panpan.consumer.config.PanPanProperties;
import com.aidenwu.esl.panpan.consumer.domain.CommandStatus;
import com.aidenwu.esl.panpan.consumer.domain.CommandTask;
import com.aidenwu.esl.panpan.consumer.domain.MessageType;
import com.aidenwu.esl.panpan.consumer.mqtt.MqttPublisher;
import com.aidenwu.esl.panpan.consumer.report.MqttReportEvent;
import com.aidenwu.esl.panpan.consumer.report.ReportType;
import com.aidenwu.esl.panpan.consumer.report.RoutedTopic;
import com.aidenwu.esl.panpan.consumer.repository.CommandEventLogRepository;
import com.aidenwu.esl.panpan.consumer.repository.CommandTaskRepository;
import com.aidenwu.esl.panpan.consumer.repository.EslTagRepository;
import com.aidenwu.esl.panpan.consumer.service.CommandTaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
class PanPanConsumerIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("esl_panpan")
            .withUsername("panpan")
            .withPassword("panpan");

    @Container
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3.13-management")
            .withUser("panpan", "panpan")
            .withPermission("/", "panpan", ".*", ".*", ".*");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "panpan");
        registry.add("spring.rabbitmq.password", () -> "panpan");
        registry.add("panpan.mqtt.enabled", () -> "false");
        registry.add("panpan.timeout.enabled", () -> "false");
    }

    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    PanPanProperties properties;
    @Autowired
    CommandTaskRepository commandTaskRepository;
    @Autowired
    CommandEventLogRepository eventLogRepository;
    @Autowired
    EslTagRepository eslTagRepository;
    @Autowired
    CommandTaskService commandTaskService;
    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    MqttPublisher mqttPublisher;

    @BeforeEach
    void setUp() {
        reset(mqttPublisher);
    }

    @Test
    void consumesRabbitCommandPublishesMqttAndSkipsDuplicateTaskUuid() {
        String taskUuid = UUID.randomUUID().toString();
        sendCommand(tagUpdateJson(taskUuid, "6597069770841"));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            CommandTask task = commandTaskRepository.findByTaskUuid(taskUuid).orElseThrow();
            assertThat(task.getStatus()).isEqualTo(CommandStatus.PUBLISHED);
            assertThat(task.getMqttTopic()).isEqualTo("esl/server/data/ZH01");
            assertThat(task.getMqttPayload()).contains("\"command\":\"wtag\"");
        });
        verify(mqttPublisher, timeout(5000).times(1)).publish(eq("esl/server/data/ZH01"), contains("\"command\":\"wtag\""));

        sendCommand(tagUpdateJson(taskUuid, "6597069770841"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(eventLogRepository.findByTaskUuidOrderByCreatedAtAsc(taskUuid))
                        .anyMatch(log -> "IDEMPOTENT_SKIP".equals(log.getEventType()))
        );
        verify(mqttPublisher, timeout(1000).times(1)).publish(eq("esl/server/data/ZH01"), contains("\"command\":\"wtag\""));
    }

    @Test
    void handlesAckAndEslReportStatusFlow() {
        String taskUuid = UUID.randomUUID().toString();
        String tagId = "6597069770842";
        sendCommand(tagUpdateJson(taskUuid, tagId));
        awaitTaskStatus(taskUuid, CommandStatus.PUBLISHED);

        sendReport(new MqttReportEvent(
                new RoutedTopic(ReportType.AP_ACK, "esl/ap/ack/ESLAP00000008", "ack", "ESLAP00000008", null),
                "{\"id\":\"" + taskUuid + "\",\"code\":0}",
                Instant.now()
        ));
        awaitTaskStatus(taskUuid, CommandStatus.AP_ACKED);

        sendReport(new MqttReportEvent(
                new RoutedTopic(ReportType.ESL_REPORT, "esl/ap/report/tag/ESLAP00000008", "tag", "ESLAP00000008", null),
                "{\"tag\":\"" + tagId + "\",\"ssirs\":-42,\"batterysoc\":92,\"tempt\":23.5,\"ap\":\"ESLAP00000008\",\"shop\":\"ZH01\",\"stat\":4}",
                Instant.now()
        ));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            CommandTask task = commandTaskRepository.findByTaskUuid(taskUuid).orElseThrow();
            assertThat(task.getStatus()).isEqualTo(CommandStatus.ESL_REPORTED);
            assertThat(eslTagRepository.findByTagId(tagId)).hasValueSatisfying(tag -> {
                assertThat(tag.getStatRaw()).isEqualTo("4");
                assertThat(tag.getBatterySoc()).isEqualTo(92);
                assertThat(tag.getApCode()).isEqualTo("ESLAP00000008");
            });
        });
    }

    @Test
    void handlesReqkeyByCreatingInternalTagKeyReplyTask() {
        String tagId = "1234567890";
        sendReport(new MqttReportEvent(
                new RoutedTopic(ReportType.REQ_KEY, "esl/esl/reqkey/ZH01", "reqkey", null, "ZH01"),
                "{\"tag\":\"" + tagId + "\",\"ap\":\"ESLAP00000009\"}",
                Instant.now()
        ));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            List<CommandTask> replies = commandTaskRepository.findByMessageType(MessageType.TAG_KEY_REPLY);
            assertThat(replies).anySatisfy(task -> {
                assertThat(task.getTagId()).isEqualTo(tagId);
                assertThat(task.getStatus()).isEqualTo(CommandStatus.PUBLISHED);
                assertThat(task.getMqttPayload()).contains("\"command\":\"dkey\"");
            });
        });
        verify(mqttPublisher, atLeastOnce()).publish(eq("esl/server/data/ZH01"), contains("\"command\":\"dkey\""));
    }

    @Test
    void timeoutScannerMarksExpiredActiveTasks() {
        String taskUuid = UUID.randomUUID().toString();
        sendCommand(tagUpdateJson(taskUuid, "6597069770843"));
        awaitTaskStatus(taskUuid, CommandStatus.PUBLISHED);
        CommandTask task = commandTaskRepository.findByTaskUuid(taskUuid).orElseThrow();
        task.setDeadlineAt(Instant.now().minusSeconds(1));
        commandTaskRepository.saveAndFlush(task);

        int timedOut = commandTaskService.markTimedOut();

        assertThat(timedOut).isGreaterThanOrEqualTo(1);
        assertThat(commandTaskRepository.findByTaskUuid(taskUuid).orElseThrow().getStatus())
                .isEqualTo(CommandStatus.TIMEOUT);
    }

    @Test
    void sendsInvalidJsonToDeadQueue() {
        sendCommand("{bad-json");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Message dead = rabbitTemplate.receive(properties.getRabbit().getDeadQueue(), 1000);
            assertThat(dead).isNotNull();
            assertThat(new String(dead.getBody(), StandardCharsets.UTF_8)).contains("bad-json");
        });
    }

    private void awaitTaskStatus(String taskUuid, CommandStatus status) {
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(commandTaskRepository.findByTaskUuid(taskUuid).orElseThrow().getStatus()).isEqualTo(status)
        );
    }

    private void sendCommand(String json) {
        Message message = MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .build();
        rabbitTemplate.send(
                properties.getRabbit().getCommandExchange(),
                properties.getRabbit().getCommandRoutingKey(),
                message
        );
    }

    private void sendReport(MqttReportEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            Message message = MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .build();
            rabbitTemplate.send(
                    properties.getRabbit().getReportExchange(),
                    properties.getRabbit().getReportRoutingKey(),
                    message
            );
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String tagUpdateJson(String taskUuid, String tagId) {
        return """
                {
                  "messageType": "TAG_UPDATE",
                  "brand": "PANPAN",
                  "shopCode": "ZH01",
                  "apCode": "ESLAP00000008",
                  "tagId": "%s",
                  "templateName": "PRICEPROMO",
                  "screenCode": "06",
                  "modelDecimal": 6,
                  "forceRefresh": 1,
                  "product": {
                    "productName": "脉动 维生素饮料青柠口味 600ML",
                    "productCode": "6902538004045",
                    "price": "10.80",
                    "brand": "",
                    "spec": "600ML",
                    "qrContent": "esl.wdyc.cn",
                    "promoPrice": null
                  },
                  "taskUuid": "%s",
                  "vendorTaskId": 39138
                }
                """.formatted(tagId, taskUuid);
    }
}
