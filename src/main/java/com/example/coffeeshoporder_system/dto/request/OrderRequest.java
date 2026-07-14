package com.example.coffeeshoporder_system.dto.request;

public record OrderRequest(
        Long userId,
        String requestId,
        Long menuId
) {
}
