package com.example.coffeeshoporder_system.dto.response;

public record MenuResponse(
        Long menuId,
        String name,
        Long price
) {
}
