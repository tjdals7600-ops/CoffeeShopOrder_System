package com.example.coffeeshoporder_system.dto.request;

public record PointChargeRequest(
        // 포인트를 충전할 사용자 식별값입니다.
        Long userId,

        // 충전 요청의 중복 반영을 막는 멱등성 키입니다.
        String requestId,

        // 충전 금액입니다. 1원은 1P로 반영됩니다.
        Long amount
) {
}
