package com.example.coffeeshoporder_system.domain.entity;

import com.example.coffeeshoporder_system.domain.type.MenuStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "menu",
        indexes = {
                @Index(name = "idx_menu_status", columnList = "status")
        }
)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu extends BaseTimeEntity {

    // 메뉴 기본 키입니다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 고객에게 표시되는 메뉴명입니다.
    @Column(nullable = false, length = 100)
    private String name;

    // 주문 시 DB 기준으로 다시 조회하는 현재 판매 가격입니다.
    @Column(nullable = false)
    private Long price;

    // 메뉴 설명입니다.
    @Column(nullable = false, length = 500)
    private String description;

    // 판매 중인 메뉴만 주문할 수 있도록 상태를 구분합니다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MenuStatus status;
}
