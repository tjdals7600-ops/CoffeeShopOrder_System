package com.example.coffeeshoporder_system.repository;

import com.example.coffeeshoporder_system.domain.entity.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByUserIdAndRequestId(Long userId, String requestId);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
