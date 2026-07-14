package com.example.coffeeshoporder_system.dto.response;

public record PopularMenuResponse(
        Long menuId,
        String name,
        Long price,
        Long orderCount,
        Long totalQuantity
) {
}
