package com.example.coffeeshoporder_system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final DataCollectionPlatformClient dataCollectionPlatformClient;

    // 주문 트랜잭션이 성공적으로 커밋된 뒤 데이터 수집 플랫폼으로 이벤트를 전송합니다.
    public void publishAfterCommit(OrderPaidEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sendSafely(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendSafely(event);
            }
        });
    }

    // 외부 전송 실패가 주문 성공 응답을 깨지 않도록 로그만 남깁니다.
    private void sendSafely(OrderPaidEvent event) {
        try {
            dataCollectionPlatformClient.send(event);
        } catch (Exception exception) {
            log.error("Failed to send order paid event. requestId={}", event.requestId(), exception);
        }
    }
}
