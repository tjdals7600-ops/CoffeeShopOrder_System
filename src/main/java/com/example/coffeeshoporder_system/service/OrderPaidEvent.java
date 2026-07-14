package com.example.coffeeshoporder_system.service;

public record OrderPaidEvent(
        Long orderId,
        Long userId,
        Long menuId,
        String menuName,
        Long unitPrice,
        Integer quantity,
        Long totalPrice,
        String requestId
) {
}
