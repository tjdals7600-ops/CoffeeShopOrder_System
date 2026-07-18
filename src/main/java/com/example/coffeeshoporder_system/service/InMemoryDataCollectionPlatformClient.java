package com.example.coffeeshoporder_system.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class InMemoryDataCollectionPlatformClient implements DataCollectionPlatformClient {

    // 외부 시스템 없이 afterCommit 이벤트 전송 여부를 검증하기 위한 테스트 전용 저장소입니다.
    private final List<OrderPaidEvent> sentEvents = new CopyOnWriteArrayList<>();

    // 테스트에서는 전송된 이벤트를 메모리에 누적합니다.
    @Override
    public void send(OrderPaidEvent event) {
        sentEvents.add(event);
    }

    // 테스트 코드가 전송된 이벤트 snapshot을 확인할 수 있게 복사본을 반환합니다.
    public List<OrderPaidEvent> getSentEvents() {
        return List.copyOf(sentEvents);
    }

    // 각 테스트가 독립적으로 검증되도록 누적 이벤트를 초기화합니다.
    public void clear() {
        sentEvents.clear();
    }
}
