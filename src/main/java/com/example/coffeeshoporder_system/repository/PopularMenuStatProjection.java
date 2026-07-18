package com.example.coffeeshoporder_system.repository;

public interface PopularMenuStatProjection {

    // 인기 메뉴 응답에 포함될 메뉴 ID입니다.
    Long getMenuId();

    // 인기 메뉴 응답에 포함될 메뉴 이름입니다.
    String getName();

    // 인기 메뉴 응답에 포함될 현재 가격입니다.
    Long getPrice();

    // 선택 기간 동안 집계된 주문 횟수입니다.
    Long getOrderCount();

    // 선택 기간 동안 집계된 총 주문 수량입니다.
    Long getTotalQuantity();

    // 선택 기간 동안 집계된 총 매출 포인트입니다.
    Long getTotalSalesPoint();
}
