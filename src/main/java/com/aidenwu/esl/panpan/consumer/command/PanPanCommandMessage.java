package com.aidenwu.esl.panpan.consumer.command;

import com.aidenwu.esl.panpan.consumer.domain.MessageType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PanPanCommandMessage {

    private MessageType messageType;
    private String brand;
    private String apCode;
    private String shopCode;
    private Long shopId;
    private Integer shopNo;
    private String taskUuid;
    private Boolean allowReplay;
    private Long vendorTaskId;

    private String templateName;
    private String screenCode;
    private String fileName;
    private String fileMd5;
    private String tenantId;

    private String fontName;
    private String fontMd5;
    private String downloadUrl;

    private String tagId;
    private Integer modelDecimal;
    private Integer forceRefresh;
    private ProductPayload product;

    private String sk;
    private String tk;

    public boolean allowReplayValue() {
        return Boolean.TRUE.equals(allowReplay);
    }
}
