package com.example.coffeeshoporder_system.repository;

import com.example.coffeeshoporder_system.domain.entity.OrderOutbox;
import com.example.coffeeshoporder_system.domain.type.OutboxStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderOutboxRepository extends JpaRepository<OrderOutbox, Long> {

    // outbox worker가 처리 대기 이벤트를 오래된 순서대로 가져갈 때 사용합니다.
    List<OrderOutbox> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);
}
