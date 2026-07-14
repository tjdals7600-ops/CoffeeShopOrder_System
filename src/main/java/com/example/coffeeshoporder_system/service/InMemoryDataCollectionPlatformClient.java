package com.example.coffeeshoporder_system.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

@Component
public class InMemoryDataCollectionPlatformClient implements DataCollectionPlatformClient {

    private final List<OrderPaidEvent> sentEvents = new CopyOnWriteArrayList<>();

    @Override
    public void send(OrderPaidEvent event) {
        sentEvents.add(event);
    }

    public List<OrderPaidEvent> getSentEvents() {
        return List.copyOf(sentEvents);
    }

    public void clear() {
        sentEvents.clear();
    }
}
