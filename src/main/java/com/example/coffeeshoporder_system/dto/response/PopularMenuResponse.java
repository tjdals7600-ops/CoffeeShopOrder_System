package com.example.coffeeshoporder_system.dto.response;

public record PopularMenuResponse(
        // 인기 순위에 포함된 메뉴 ID입니다.
        Long menuId,

        // 메뉴 이름입니다.
        String name,

        // 현재 메뉴 가격입니다.
        Long price,

        // 선택된 기간 동안 해당 메뉴가 주문된 횟수입니다.
        Long orderCount,

        // 선택된 기간 동안 해당 메뉴가 주문된 총 수량입니다.
        Long totalQuantity
) {
}
