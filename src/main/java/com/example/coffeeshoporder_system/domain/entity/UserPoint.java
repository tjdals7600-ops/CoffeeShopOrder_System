package com.example.coffeeshoporder_system.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "user_point",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_point_user_id", columnNames = "user_id")
        }
)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPoint extends BaseTimeEntity {

    // 사용자 포인트 row의 기본 키입니다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 별도 User 엔티티 없이 사용자 식별값을 스칼라 값으로 저장합니다.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 현재 사용 가능한 포인트 잔액입니다.
    @Column(nullable = false)
    private Long balance;

    // 포인트 충전 금액을 잔액에 더합니다.
    public void charge(Long amount) {
        balance += amount;
    }

    // 주문 결제 전 잔액이 충분한지 확인합니다.
    public boolean hasEnoughBalance(Long amount) {
        return balance >= amount;
    }

    // 주문 결제 금액만큼 포인트를 차감합니다.
    public void use(Long amount) {
        balance -= amount;
    }
}
