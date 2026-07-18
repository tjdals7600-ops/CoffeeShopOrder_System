package com.example.coffeeshoporder_system.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "order_item",
        indexes = {
                @Index(name = "idx_order_item_menu_id", columnList = "menu_id")
        }
)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    // 주문 항목 기본 키입니다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 주문에 포함된 항목인지 나타냅니다.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_item_order"))
    private Order order;

    // 어떤 메뉴를 주문했는지 나타냅니다.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_item_menu"))
    private Menu menu;

    // 메뉴명이 나중에 바뀌어도 주문 당시 이름을 보존합니다.
    @Column(name = "menu_name_snapshot", nullable = false, length = 100)
    private String menuNameSnapshot;

    // 메뉴 가격이 나중에 바뀌어도 주문 당시 가격을 보존합니다.
    @Column(name = "menu_price_snapshot", nullable = false)
    private Long menuPriceSnapshot;

    // 해당 메뉴의 주문 수량입니다.
    @Column(nullable = false)
    private Integer quantity;

    // 주문 당시 단가와 수량으로 계산한 항목 금액입니다.
    @Column(name = "line_price", nullable = false)
    private Long linePrice;
}
