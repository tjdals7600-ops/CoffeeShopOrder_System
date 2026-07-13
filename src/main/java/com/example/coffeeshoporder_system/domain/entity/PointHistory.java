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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PointHistoryType type;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    @Column(nullable = false, length = 100)
    private String reason;

    @Column(name = "request_id", nullable = false, length = 100)
    private String requestId;
}
