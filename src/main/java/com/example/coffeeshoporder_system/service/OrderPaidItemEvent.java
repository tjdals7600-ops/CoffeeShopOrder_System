package com.example.coffeeshoporder_system.service;

public record OrderPaidItemEvent(
        Long menuId,
        String menuName,
        Long unitPrice,
        Integer quantity,
        Long linePrice
) {
}
