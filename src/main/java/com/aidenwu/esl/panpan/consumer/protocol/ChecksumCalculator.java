package com.aidenwu.esl.panpan.consumer.protocol;

import com.aidenwu.esl.panpan.consumer.command.PanPanCommandMessage;
import java.util.Map;

public interface ChecksumCalculator {

    String calculate(PanPanCommandMessage message, Map<String, Object> value);
}
