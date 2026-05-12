package com.aidenwu.esl.panpan.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class EslPanpanProtocolConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EslPanpanProtocolConsumerApplication.class, args);
    }
}
