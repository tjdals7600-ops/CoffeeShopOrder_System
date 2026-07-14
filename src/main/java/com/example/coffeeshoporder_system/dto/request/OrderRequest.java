package com.example.coffeeshoporder_system.dto.request;

import java.util.List;

public record OrderRequest(
        Long userId,
        String requestId,
        Long menuId,
        List<OrderItemRequest> items
) {
}
