package com.example.coffeeshoporder_system.repository;

import com.example.coffeeshoporder_system.domain.entity.OrderOutbox;
import com.example.coffeeshoporder_system.domain.type.OutboxStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderOutboxRepository extends JpaRepository<OrderOutbox, Long> {

    List<OrderOutbox> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);
}
