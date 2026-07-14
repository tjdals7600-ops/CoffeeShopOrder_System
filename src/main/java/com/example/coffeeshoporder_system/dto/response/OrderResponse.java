package com.example.coffeeshoporder_system.dto.response;

import java.util.List;

public record OrderResponse(
        Long orderId,
        String status,
        Long totalPrice,
        Long balance,
        List<OrderItemResponse> items
) {
}
