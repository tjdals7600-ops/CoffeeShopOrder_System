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

    private void sendSafely(OrderPaidEvent event) {
        try {
            dataCollectionPlatformClient.send(event);
        } catch (Exception exception) {
            log.error("Failed to send order paid event. requestId={}", event.requestId(), exception);
        }
    }
}
