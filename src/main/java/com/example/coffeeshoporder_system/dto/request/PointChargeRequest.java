package com.example.coffeeshoporder_system.dto.request;

public record PointChargeRequest(
        Long userId,
        String requestId,
        Long amount
) {
}
