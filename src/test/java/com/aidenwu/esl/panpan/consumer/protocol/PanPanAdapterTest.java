package com.aidenwu.esl.panpan.consumer.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aidenwu.esl.panpan.consumer.command.PanPanCommandMessage;
import com.aidenwu.esl.panpan.consumer.command.ProductPayload;
import com.aidenwu.esl.panpan.consumer.config.PanPanProperties;
import com.aidenwu.esl.panpan.consumer.domain.MessageType;
import com.aidenwu.esl.panpan.consumer.protocol.builder.PanPanDkeyBuilder;
import com.aidenwu.esl.panpan.consumer.protocol.builder.PanPanFontsBuilder;
import com.aidenwu.esl.panpan.consumer.protocol.builder.PanPanPayloadBuilder;
import com.aidenwu.esl.panpan.consumer.protocol.builder.PanPanShopcodeBuilder;
import com.aidenwu.esl.panpan.consumer.protocol.builder.PanPanTmsyncBuilder;
import com.aidenwu.esl.panpan.consumer.protocol.builder.PanPanTmpllistBuilder;
import com.aidenwu.esl.panpan.consumer.protocol.builder.PanPanWtagBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PanPanAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private PanPanAdapter adapter;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(1706271905137L), ZoneOffset.UTC);
        PanPanProperties properties = new PanPanProperties();
        DefaultChecksumCalculator checksumCalculator = new DefaultChecksumCalculator(objectMapper);
        DefaultTokenProvider tokenProvider = new DefaultTokenProvider(properties);
        List<PanPanPayloadBuilder> builders = List.of(
                new PanPanShopcodeBuilder(objectMapper, clock),
                new PanPanTmsyncBuilder(objectMapper, clock),
                new PanPanTmpllistBuilder(objectMapper, clock),
                new PanPanFontsBuilder(objectMapper, clock),
                new PanPanWtagBuilder(objectMapper, clock, checksumCalculator, tokenProvider),
                new PanPanDkeyBuilder(objectMapper, clock)
        );
        adapter = new PanPanAdapter(new CommandValidator(), builders);
    }

    @Test
    void buildsShopcodePayload() throws Exception {
        PanPanCommandMessage message = new PanPanCommandMessage();
        message.setMessageType(MessageType.AP_BIND_SHOP);
        message.setTaskUuid("903d5240-5770-49f3-acdd-43fa4f0034e5");
        message.setApCode("ESLAP00000008");
        message.setShopCode("ZH01");
        message.setShopId(1L);
        message.setShopNo(1);

        MqttCommand command = adapter.adapt(message);
        JsonNode payload = objectMapper.readTree(command.payload());

        assertThat(command.topic()).isEqualTo("esl/server/mgr/ESLAP00000008");
        assertThat(payload.get("op").asText()).isEqualTo("shopcode");
        assertThat(payload.get("shopcode").asText()).isEqualTo("ZH01");
        assertThat(payload.get("timestamp").asLong()).isEqualTo(1706271905137L);
    }

    @Test
    void buildsWtagPayloadWithChecksumAndToken() throws Exception {
        PanPanCommandMessage message = new PanPanCommandMessage();
        message.setMessageType(MessageType.TAG_UPDATE);
        message.setTaskUuid("3db4b81b-da87-4aa1-b8bb-ab2adf785558");
        message.setShopCode("ZH01");
        message.setApCode("ESLAP00000008");
        message.setTagId("6597069770841");
        message.setTemplateName("PRICEPROMO");
        message.setScreenCode("06");
        message.setForceRefresh(1);
        message.setVendorTaskId(39138L);
        ProductPayload product = new ProductPayload();
        product.setProductName("脉动 维生素饮料青柠口味 600ML");
        product.setProductCode("6902538004045");
        product.setPrice(new BigDecimal("10.80"));
        product.setSpec("600ML");
        product.setQrContent("esl.wdyc.cn");
        message.setProduct(product);

        MqttCommand command = adapter.adapt(message);
        JsonNode payload = objectMapper.readTree(command.payload());

        assertThat(command.topic()).isEqualTo("esl/server/data/ZH01");
        assertThat(payload.get("command").asText()).isEqualTo("wtag");
        assertThat(payload.get("model").asInt()).isEqualTo(6);
        assertThat(payload.get("forcefrash").asInt()).isEqualTo(1);
        assertThat(payload.get("taskid").asLong()).isEqualTo(39138L);
        assertThat(payload.get("checksum").asText()).hasSize(32);
        assertThat(payload.get("token").asText()).hasSize(32);
        assertThat(payload.get("value").get("GOODS_CODE").asText()).isEqualTo("6902538004045");
        assertThat(payload.get("value").get("F_1").asText()).isEqualTo("10.80");
    }

    @Test
    void buildsDkeyPayload() throws Exception {
        PanPanCommandMessage message = new PanPanCommandMessage();
        message.setMessageType(MessageType.TAG_KEY_REPLY);
        message.setTaskUuid("uuid");
        message.setShopCode("ZH01");
        message.setTagId("1234567890");
        message.setSk("sk-value");
        message.setTk("tk-value");

        MqttCommand command = adapter.adapt(message);
        JsonNode payload = objectMapper.readTree(command.payload());

        assertThat(command.topic()).isEqualTo("esl/server/data/ZH01");
        assertThat(payload.get("command").asText()).isEqualTo("dkey");
        assertThat(payload.get("tag").asText()).isEqualTo("1234567890");
        assertThat(payload.get("sk").asText()).isEqualTo("sk-value");
    }

    @Test
    void rejectsMissingRequiredField() {
        PanPanCommandMessage message = new PanPanCommandMessage();
        message.setMessageType(MessageType.TAG_UPDATE);
        message.setTaskUuid("uuid");
        message.setShopCode("ZH01");

        assertThatThrownBy(() -> adapter.adapt(message))
                .isInstanceOf(ProtocolException.class)
                .hasMessageContaining("tagId is required");
    }
}
