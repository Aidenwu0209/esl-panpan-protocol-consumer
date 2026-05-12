package com.aidenwu.esl.panpan.consumer.device;

import com.aidenwu.esl.panpan.consumer.command.CommandRabbitProducer;
import com.aidenwu.esl.panpan.consumer.domain.ApDevice;
import com.aidenwu.esl.panpan.consumer.domain.EslTag;
import com.aidenwu.esl.panpan.consumer.domain.TagKey;
import com.aidenwu.esl.panpan.consumer.report.MqttReportEvent;
import com.aidenwu.esl.panpan.consumer.repository.ApDeviceRepository;
import com.aidenwu.esl.panpan.consumer.repository.EslTagRepository;
import com.aidenwu.esl.panpan.consumer.service.CommandTaskService;
import com.aidenwu.esl.panpan.consumer.service.EventLogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DeviceReportService {

    private final ObjectMapper objectMapper;
    private final ApDeviceRepository apDeviceRepository;
    private final EslTagRepository eslTagRepository;
    private final CommandTaskService commandTaskService;
    private final TagKeyService tagKeyService;
    private final CommandRabbitProducer commandRabbitProducer;
    private final EventLogService eventLogService;
    private final Clock clock;

    public DeviceReportService(
            ObjectMapper objectMapper,
            ApDeviceRepository apDeviceRepository,
            EslTagRepository eslTagRepository,
            CommandTaskService commandTaskService,
            TagKeyService tagKeyService,
            CommandRabbitProducer commandRabbitProducer,
            EventLogService eventLogService,
            Clock clock
    ) {
        this.objectMapper = objectMapper;
        this.apDeviceRepository = apDeviceRepository;
        this.eslTagRepository = eslTagRepository;
        this.commandTaskService = commandTaskService;
        this.tagKeyService = tagKeyService;
        this.commandRabbitProducer = commandRabbitProducer;
        this.eventLogService = eventLogService;
        this.clock = clock;
    }

    @Transactional
    public void handleHeartbeat(MqttReportEvent event) {
        JsonNode payload = readPayload(event);
        String apCode = firstText(payload, event.getClientId(), "ap", "apCode", "clientid");
        ApDevice ap = apDeviceRepository.findByApCode(apCode).orElseGet(ApDevice::new);
        ap.setApCode(apCode);
        ap.setClientId(event.getClientId());
        ap.setOnline(true);
        ap.setShopCode(firstText(payload, event.getShopCode(), "shopcode", "shopCode", "shop"));
        ap.setShopId(firstLong(payload, "shopid", "shopId"));
        ap.setShopNo(firstInt(payload, "shopno", "shopNo"));
        ap.setHeartbeatIndex(firstLong(payload, "index", "idx", "heartbeatIndex"));
        ap.setLastHeartbeatAt(resolveReportTime(payload, event));
        ap.setLastPayload(event.getRawPayload());
        apDeviceRepository.save(ap);
        eventLogService.log(null, "AP_HEARTBEAT", null, null, event.getTopic(), event.getRawPayload(), apCode, null);
    }

    @Transactional
    public void handleRuninfo(MqttReportEvent event) {
        JsonNode payload = readPayload(event);
        String apCode = firstText(payload, event.getClientId(), "ap", "apCode", "clientid");
        ApDevice ap = apDeviceRepository.findByApCode(apCode).orElseGet(ApDevice::new);
        ap.setApCode(apCode);
        ap.setClientId(event.getClientId());
        ap.setCpuUsage(firstDouble(payload, "cpu", "cpuUsage"));
        ap.setMemoryUsage(firstDouble(payload, "mem", "memory", "memoryUsage"));
        ap.setDiskUsage(firstDouble(payload, "disk", "diskUsage"));
        ap.setIpAddress(firstText(payload, null, "ip", "ipAddress"));
        ap.setMacAddress(firstText(payload, null, "mac", "macAddress"));
        ap.setFirmwareVersion(firstText(payload, null, "version", "firmwareVersion"));
        ap.setAppVersion(firstText(payload, null, "appVersion"));
        ap.setLastPayload(event.getRawPayload());
        apDeviceRepository.save(ap);
        eventLogService.log(null, "AP_RUNINFO", null, null, event.getTopic(), event.getRawPayload(), apCode, null);
    }

    @Transactional
    public void handleEslReport(MqttReportEvent event) {
        JsonNode payload = readPayload(event);
        for (JsonNode tagNode : extractTagNodes(payload)) {
            String tagId = firstText(tagNode, null, "tag", "tagId");
            if (!StringUtils.hasText(tagId)) {
                continue;
            }
            EslTag tag = eslTagRepository.findByTagId(tagId).orElseGet(EslTag::new);
            tag.setTagId(tagId);
            tag.setApCode(firstText(tagNode, event.getClientId(), "ap", "apCode"));
            tag.setShopCode(firstText(tagNode, event.getShopCode(), "shop", "shopCode", "shopcode"));
            tag.setBatterySoc(firstInt(tagNode, "batterysoc", "batterySoc"));
            tag.setSsirs(firstInt(tagNode, "ssirs"));
            tag.setTemperature(firstDecimal(tagNode, "tempt", "temperature"));
            JsonNode stat = firstNode(tagNode, "stat");
            tag.setStatRaw(stat == null || stat.isMissingNode() || stat.isNull() ? null : stat.asText());
            tag.setLastReportAt(resolveReportTime(tagNode, event));
            tag.setLastPayload(event.getRawPayload());
            eslTagRepository.save(tag);

            String taskUuid = firstText(tagNode, null, "id", "taskid", "taskUuid");
            if (StringUtils.hasText(taskUuid)) {
                commandTaskService.markEslReported(taskUuid, event.getTopic(), event.getRawPayload());
            } else {
                commandTaskService.markLatestTagTaskEslReported(tagId, event.getTopic(), event.getRawPayload());
            }
        }
        eventLogService.log(null, "ESL_REPORT_HANDLED", null, null, event.getTopic(), event.getRawPayload(), null, null);
    }

    @Transactional
    public void handleAck(MqttReportEvent event) {
        JsonNode payload = readPayload(event);
        String taskUuid = firstText(payload, null, "id", "taskid", "taskUuid");
        if (!StringUtils.hasText(taskUuid)) {
            throw new IllegalArgumentException("ACK payload missing id/taskid/taskUuid");
        }
        commandTaskService.markApAcked(taskUuid, event.getTopic(), event.getRawPayload());
    }

    @Transactional
    public void handleReqkey(MqttReportEvent event) {
        JsonNode payload = readPayload(event);
        String tagId = firstText(payload, null, "tag", "tagId");
        if (!StringUtils.hasText(tagId)) {
            throw new IllegalArgumentException("REQ_KEY payload missing tag/tagId");
        }
        String shopCode = firstText(payload, event.getShopCode(), "shop", "shopCode", "shopcode");
        if (!StringUtils.hasText(shopCode)) {
            throw new IllegalArgumentException("REQ_KEY missing shop code");
        }
        String apCode = firstText(payload, null, "ap", "apCode", "clientid");
        TagKey tagKey = tagKeyService.getOrCreateForReqkey(tagId, shopCode, apCode);
        String taskUuid = commandRabbitProducer.enqueueTagKeyReply(shopCode, apCode, tagId, tagKey);
        eventLogService.log(taskUuid, "REQ_KEY_REPLY_QUEUED", null, null, event.getTopic(), event.getRawPayload(), null, null);
    }

    private JsonNode readPayload(MqttReportEvent event) {
        try {
            if (!StringUtils.hasText(event.getRawPayload())) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.readTree(event.getRawPayload());
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid MQTT report JSON payload", e);
        }
    }

    private List<JsonNode> extractTagNodes(JsonNode payload) {
        if (payload.isArray()) {
            List<JsonNode> nodes = new ArrayList<>();
            payload.forEach(nodes::add);
            return nodes;
        }
        for (String field : List.of("tags", "data", "list")) {
            JsonNode node = payload.get(field);
            if (node != null && node.isArray()) {
                List<JsonNode> nodes = new ArrayList<>();
                node.forEach(nodes::add);
                return nodes;
            }
        }
        return List.of(payload);
    }

    private Instant resolveReportTime(JsonNode payload, MqttReportEvent event) {
        Long millis = firstLong(payload, "timestamp", "time");
        if (millis != null && millis > 0) {
            return Instant.ofEpochMilli(millis);
        }
        return event.getReceivedAt() == null ? Instant.now(clock) : event.getReceivedAt();
    }

    private String firstText(JsonNode node, String fallback, String... fields) {
        for (String field : fields) {
            JsonNode value = firstNode(node, field);
            if (value != null && !value.isMissingNode() && !value.isNull() && StringUtils.hasText(value.asText())) {
                return value.asText();
            }
        }
        return fallback;
    }

    private Integer firstInt(JsonNode node, String... fields) {
        JsonNode value = firstNode(node, fields);
        return value == null || value.isMissingNode() || value.isNull() ? null : value.asInt();
    }

    private Long firstLong(JsonNode node, String... fields) {
        JsonNode value = firstNode(node, fields);
        return value == null || value.isMissingNode() || value.isNull() ? null : value.asLong();
    }

    private Double firstDouble(JsonNode node, String... fields) {
        JsonNode value = firstNode(node, fields);
        return value == null || value.isMissingNode() || value.isNull() ? null : value.asDouble();
    }

    private BigDecimal firstDecimal(JsonNode node, String... fields) {
        JsonNode value = firstNode(node, fields);
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.decimalValue();
    }

    private JsonNode firstNode(JsonNode node, String... fields) {
        if (node == null) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
