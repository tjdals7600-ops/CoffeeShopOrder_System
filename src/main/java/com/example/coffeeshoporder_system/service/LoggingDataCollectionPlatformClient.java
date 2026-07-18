package com.example.coffeeshoporder_system.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
public class LoggingDataCollectionPlatformClient implements DataCollectionPlatformClient {

    // 운영 환경의 실제 HTTP/Kafka 연동 전까지 주문 완료 이벤트를 로그로 남깁니다.
    @Override
    public void send(OrderPaidEvent event) {

        log.info("Order paid event sent to data collection platform. event={}", event);
    }
}
