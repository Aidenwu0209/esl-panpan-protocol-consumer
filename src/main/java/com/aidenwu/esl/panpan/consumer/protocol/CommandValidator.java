package com.aidenwu.esl.panpan.consumer.protocol;

import com.aidenwu.esl.panpan.consumer.command.PanPanCommandMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CommandValidator {

    public void validate(PanPanCommandMessage message) {
        if (message == null) {
            throw new ProtocolException("Command message is empty");
        }
        if (message.getMessageType() == null) {
            throw new ProtocolException("messageType is required");
        }
        require(message.getTaskUuid(), "taskUuid");

        switch (message.getMessageType()) {
            case AP_BIND_SHOP -> {
                require(message.getApCode(), "apCode");
                require(message.getShopCode(), "shopCode");
                require(message.getShopId(), "shopId");
                require(message.getShopNo(), "shopNo");
            }
            case AP_TIME_SYNC -> require(message.getApCode(), "apCode");
            case TEMPLATE_PUBLISH -> {
                require(message.getShopCode(), "shopCode");
                require(message.getTemplateName(), "templateName");
                require(message.getScreenCode(), "screenCode");
                require(message.getFileName(), "fileName");
                require(message.getFileMd5(), "fileMd5");
                require(message.getDownloadUrl(), "downloadUrl");
            }
            case FONT_PUBLISH -> {
                require(message.getShopCode(), "shopCode");
                require(message.getFontName(), "fontName");
                require(message.getFontMd5(), "fontMd5");
                require(message.getDownloadUrl(), "downloadUrl");
            }
            case TAG_UPDATE -> {
                require(message.getShopCode(), "shopCode");
                require(message.getTagId(), "tagId");
                require(message.getTemplateName(), "templateName");
                require(message.getProduct(), "product");
            }
            case TAG_KEY_REPLY -> {
                require(message.getShopCode(), "shopCode");
                require(message.getTagId(), "tagId");
                require(message.getSk(), "sk");
                require(message.getTk(), "tk");
            }
        }
    }

    private void require(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new ProtocolException(field + " is required");
        }
    }

    private void require(Object value, String field) {
        if (value == null) {
            throw new ProtocolException(field + " is required");
        }
    }
}
