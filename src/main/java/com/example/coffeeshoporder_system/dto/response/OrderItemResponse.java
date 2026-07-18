package com.example.coffeeshoporder_system.dto.response;

public record OrderItemResponse(
        // 주문한 메뉴 ID입니다.
        Long menuId,

        // 주문 시점에 저장한 메뉴명 snapshot입니다.
        String menuName,

        // 주문 시점에 저장한 단가 snapshot입니다.
        Long unitPrice,

        // 주문 수량입니다.
        Integer quantity,

        // 단가와 수량을 곱한 주문 항목 금액입니다.
        Long linePrice
) {
}
