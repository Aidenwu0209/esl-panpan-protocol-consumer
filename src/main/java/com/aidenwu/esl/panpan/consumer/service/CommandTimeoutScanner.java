package com.aidenwu.esl.panpan.consumer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "panpan.timeout", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CommandTimeoutScanner {

    private final CommandTaskService commandTaskService;

    public CommandTimeoutScanner(CommandTaskService commandTaskService) {
        this.commandTaskService = commandTaskService;
    }

    @Scheduled(fixedDelayString = "#{@panPanProperties.timeout.scanInterval.toMillis()}")
    public void scan() {
        int count = commandTaskService.markTimedOut();
        if (count > 0) {
            log.info("Marked {} command tasks as TIMEOUT", count);
        }
    }
}
