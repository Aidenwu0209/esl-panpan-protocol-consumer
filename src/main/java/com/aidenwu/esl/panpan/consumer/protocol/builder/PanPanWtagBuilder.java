package com.aidenwu.esl.panpan.consumer.protocol.builder;

import com.aidenwu.esl.panpan.consumer.command.PanPanCommandMessage;
import com.aidenwu.esl.panpan.consumer.command.ProductPayload;
import com.aidenwu.esl.panpan.consumer.domain.MessageType;
import com.aidenwu.esl.panpan.consumer.protocol.ChecksumCalculator;
import com.aidenwu.esl.panpan.consumer.protocol.MqttCommand;
import com.aidenwu.esl.panpan.consumer.protocol.ProtocolException;
import com.aidenwu.esl.panpan.consumer.protocol.TokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PanPanWtagBuilder extends JsonBuilderSupport implements PanPanPayloadBuilder {

    private final Clock clock;
    private final ChecksumCalculator checksumCalculator;
    private final TokenProvider tokenProvider;

    public PanPanWtagBuilder(
            ObjectMapper objectMapper,
            Clock clock,
            ChecksumCalculator checksumCalculator,
            TokenProvider tokenProvider
    ) {
        super(objectMapper);
        this.clock = clock;
        this.checksumCalculator = checksumCalculator;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public MessageType supports() {
        return MessageType.TAG_UPDATE;
    }

    @Override
    public MqttCommand build(PanPanCommandMessage message) {
        Map<String, Object> value = buildValue(message.getProduct());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("command", "wtag");
        payload.put("id", message.getTaskUuid());
        payload.put("tag", message.getTagId());
        payload.put("tmpl", message.getTemplateName());
        payload.put("model", resolveModel(message));
        payload.put("checksum", checksumCalculator.calculate(message, value));
        payload.put("forcefrash", message.getForceRefresh() == null ? 0 : message.getForceRefresh());
        payload.put("value", value);
        payload.put("taskid", message.getVendorTaskId() == null ? message.getTaskUuid() : message.getVendorTaskId());
        payload.put("token", tokenProvider.issueToken(message));
        payload.put("timestamp", clock.millis());
        return new MqttCommand("esl/server/data/" + message.getShopCode(), json(payload));
    }

    private Map<String, Object> buildValue(ProductPayload product) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("GOODS_NAME", product.getProductName());
        value.put("GOODS_CODE", product.getProductCode());
        value.put("F_1", format(product.getPrice()));
        value.put("F_2", StringUtils.hasText(product.getSpec()) ? product.getSpec() : "");
        value.put("QRCODE", product.getQrContent());
        value.put("F_20", product.getPromoPrice() == null ? null : format(product.getPromoPrice()));
        return value;
    }

    private String format(BigDecimal decimal) {
        if (decimal == null) {
            return null;
        }
        return decimal.stripTrailingZeros().scale() < 0
                ? decimal.setScale(0).toPlainString()
                : decimal.toPlainString();
    }

    private int resolveModel(PanPanCommandMessage message) {
        if (message.getModelDecimal() != null) {
            return message.getModelDecimal();
        }
        if (StringUtils.hasText(message.getScreenCode())) {
            try {
                return Integer.parseInt(message.getScreenCode(), 16);
            } catch (NumberFormatException e) {
                throw new ProtocolException("screenCode cannot be converted to decimal model: " + message.getScreenCode(), e);
            }
        }
        throw new ProtocolException("modelDecimal or screenCode is required for TAG_UPDATE");
    }
}
