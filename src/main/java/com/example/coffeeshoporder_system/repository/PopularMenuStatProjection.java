package com.example.coffeeshoporder_system.repository;

public interface PopularMenuStatProjection {

    Long getMenuId();

    String getName();

    Long getPrice();

    Long getOrderCount();

    Long getTotalQuantity();

    Long getTotalSalesPoint();
}
