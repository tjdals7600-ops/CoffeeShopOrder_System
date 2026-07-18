package com.example.coffeeshoporder_system.service;

import java.util.List;

public record OrderPaidEvent(
        // 결제가 완료된 주문 ID입니다.
        Long orderId,

        // 주문한 사용자 식별값입니다.
        Long userId,

        // 주문 총액입니다.
        Long totalPrice,

        // 멱등 처리를 위한 요청 ID입니다.
        String requestId,

        // 하위 수집 시스템이 주문 테이블을 다시 조회하지 않도록 항목 snapshot을 포함합니다.
        List<OrderPaidItemEvent> items
) {
}
