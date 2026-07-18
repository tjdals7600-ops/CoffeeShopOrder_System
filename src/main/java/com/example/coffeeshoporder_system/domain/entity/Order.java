package com.example.coffeeshoporder_system.domain.entity;

import com.example.coffeeshoporder_system.domain.type.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "orders",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_orders_user_request", columnNames = {"user_id", "request_id"})
        },
        indexes = {
                @Index(name = "idx_orders_user_created_at", columnList = "user_id, created_at")
        }
)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends CreatedAtEntity {

    // 주문 기본 키입니다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주문한 사용자 식별값입니다.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // DB 메뉴 가격과 수량으로 계산한 전체 주문 금액입니다.
    @Column(name = "total_price", nullable = false)
    private Long totalPrice;

    // 포인트 결제가 완료되면 PAID 상태로 저장합니다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    // 같은 주문 요청이 두 번 처리되지 않도록 저장하는 멱등성 키입니다.
    @Column(name = "request_id", nullable = false, length = 100)
    private String requestId;
}
