package com.example.coffeeshoporder_system.dto.request;

public record OrderItemRequest(
        // 주문할 메뉴 ID입니다. 실제 가격은 항상 DB에서 다시 조회합니다.
        Long menuId,

        // 0 이하 수량 주문을 막기 위해 양수만 허용합니다.
        Integer quantity
) {
}
