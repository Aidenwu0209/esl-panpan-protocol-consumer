package com.aidenwu.esl.panpan.consumer.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductPayload {

    private String productName;
    private String productCode;
    private BigDecimal price;
    private String brand;
    private String spec;
    private String qrContent;
    private BigDecimal promoPrice;
}
