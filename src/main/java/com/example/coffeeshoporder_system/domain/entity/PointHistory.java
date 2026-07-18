package com.example.coffeeshoporder_system.domain.entity;

import com.example.coffeeshoporder_system.domain.type.PointHistoryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
        name = "point_history",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_point_history_user_request_type",
                        columnNames = {"user_id", "request_id", "type"}
                )
        }
)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory extends CreatedAtEntity {

    // 포인트 이력 기본 키입니다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 포인트가 변경된 사용자 식별값입니다.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 충전, 사용, 취소 같은 포인트 변경 유형입니다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PointHistoryType type;

    // 변경된 포인트 금액입니다.
    @Column(nullable = false)
    private Long amount;

    // 변경이 반영된 뒤의 잔액입니다.
    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    // 이력이 발생한 업무 사유입니다.
    @Column(nullable = false, length = 100)
    private String reason;

    // 중복 요청 방지를 위한 멱등성 키입니다.
    @Column(name = "request_id", nullable = false, length = 100)
    private String requestId;
}
