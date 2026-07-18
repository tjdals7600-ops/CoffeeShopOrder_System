package com.example.coffeeshoporder_system.domain.entity;

import com.example.coffeeshoporder_system.domain.type.OutboxEventType;
import com.example.coffeeshoporder_system.domain.type.OutboxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "order_outbox",
        indexes = {
                @Index(name = "idx_order_outbox_status_created_at", columnList = "status, created_at")
        }
)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderOutbox extends CreatedAtEntity {

    // outbox 이벤트 기본 키입니다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 종류의 이벤트인지 구분합니다.
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private OutboxEventType eventType;

    // 외부 수집 플랫폼이나 worker가 사용할 이벤트 본문입니다.
    @Lob
    @Column(nullable = false)
    private String payload;

    // worker 처리 상태입니다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    // 이벤트 처리 실패 시 재시도 횟수를 기록합니다.
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;
}
