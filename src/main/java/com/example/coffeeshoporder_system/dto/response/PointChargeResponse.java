package com.example.coffeeshoporder_system.dto.response;

public record PointChargeResponse(
        Long userId,
        Long chargedAmount,
        Long balance
) {
}
