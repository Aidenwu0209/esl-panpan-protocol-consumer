package com.aidenwu.esl.panpan.consumer.protocol;

import com.aidenwu.esl.panpan.consumer.command.PanPanCommandMessage;

public interface TokenProvider {

    String issueToken(PanPanCommandMessage message);
}
