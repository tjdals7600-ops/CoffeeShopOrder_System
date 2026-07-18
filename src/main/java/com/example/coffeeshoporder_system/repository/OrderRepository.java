package com.example.coffeeshoporder_system.repository;

import com.example.coffeeshoporder_system.domain.entity.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 주문 requestId 멱등성 처리를 위해 기존 주문을 찾습니다.
    Optional<Order> findByUserIdAndRequestId(Long userId, String requestId);

    // 사용자별 최근 주문 내역 조회 확장에 사용할 수 있는 기본 조회입니다.
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
