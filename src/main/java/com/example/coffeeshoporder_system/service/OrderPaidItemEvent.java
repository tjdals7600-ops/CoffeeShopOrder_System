package com.example.coffeeshoporder_system.service;

public record OrderPaidItemEvent(
        // 주문된 메뉴 ID입니다.
        Long menuId,

        // 주문 시점의 메뉴명입니다.
        String menuName,

        // 주문 시점의 단가입니다.
        Long unitPrice,

        // 주문 수량입니다.
        Integer quantity,

        // 주문 항목별 금액 snapshot입니다.
        Long linePrice
) {
}
