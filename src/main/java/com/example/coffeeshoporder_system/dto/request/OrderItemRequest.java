package com.example.coffeeshoporder_system.dto.request;

public record OrderItemRequest(
        Long menuId,
        Integer quantity
) {
}
