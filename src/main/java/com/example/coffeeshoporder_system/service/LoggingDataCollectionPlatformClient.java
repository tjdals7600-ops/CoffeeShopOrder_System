package com.example.coffeeshoporder_system.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
public class LoggingDataCollectionPlatformClient implements DataCollectionPlatformClient {

    @Override
    public void send(OrderPaidEvent event) {
        log.info("Order paid event sent to data collection platform. event={}", event);
    }
}
