package com.example.coffeeshoporder_system.dto.response;

public record OrderItemResponse(
        Long menuId,
        String menuName,
        Long unitPrice,
        Integer quantity,
        Long linePrice
) {
}
