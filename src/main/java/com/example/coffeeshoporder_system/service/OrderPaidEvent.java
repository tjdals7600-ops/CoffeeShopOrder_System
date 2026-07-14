package com.example.coffeeshoporder_system.service;

import java.util.List;

public record OrderPaidEvent(
        Long orderId,
        Long userId,
        Long totalPrice,
        String requestId,
        List<OrderPaidItemEvent> items
) {
}
