package com.example.coffeeshoporder_system.dto.response;

public record PointChargeResponse(
        // 충전된 사용자 식별값입니다.
        Long userId,

        // 이번 요청으로 반영된 충전 금액입니다.
        Long chargedAmount,

        // 충전 후 포인트 잔액입니다.
        Long balance
) {
}
