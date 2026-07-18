package com.example.coffeeshoporder_system.dto.response;

public record MenuResponse(
        // 클라이언트가 주문할 때 사용할 메뉴 식별값입니다.
        Long menuId,

        // 화면에 표시할 메뉴 이름입니다.
        String name,

        // 현재 판매 가격입니다. 주문 시에는 DB에서 다시 조회합니다.
        Long price
) {
}
