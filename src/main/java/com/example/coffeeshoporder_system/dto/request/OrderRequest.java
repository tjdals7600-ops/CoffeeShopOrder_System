package com.example.coffeeshoporder_system.dto.request;

import java.util.List;

public record OrderRequest(
        // 별도 User 엔티티 없이 스칼라 사용자 식별값으로 관리합니다.
        Long userId,

        // 같은 요청이 반복되더라도 중복 결제되지 않도록 사용하는 멱등성 키입니다.
        String requestId,

        // 기존 단일 메뉴 주문 요청과 호환하기 위한 필드입니다.
        Long menuId,

        // README 명세의 다중 메뉴 주문 형식을 지원하는 필드입니다.
        List<OrderItemRequest> items
) {
}
