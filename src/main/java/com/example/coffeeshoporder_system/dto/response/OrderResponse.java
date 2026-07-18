package com.example.coffeeshoporder_system.dto.response;

import java.util.List;

public record OrderResponse(
        // 생성된 주문 ID입니다.
        Long orderId,

        // 주문 상태입니다. 결제가 끝난 주문은 PAID입니다.
        String status,

        // 전체 주문 금액입니다.
        Long totalPrice,

        // 결제 후 남은 포인트 잔액입니다.
        Long balance,

        // 주문 항목별 snapshot 응답입니다.
        List<OrderItemResponse> items
) {
}
