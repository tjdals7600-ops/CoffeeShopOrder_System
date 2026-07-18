package com.example.coffeeshoporder_system.repository;

public interface MenuSummaryProjection {

    // 메뉴 목록 응답에 필요한 메뉴 ID입니다.
    Long getMenuId();

    // 메뉴 목록 응답에 필요한 메뉴 이름입니다.
    String getName();

    // 메뉴 목록 응답에 필요한 현재 가격입니다.
    Long getPrice();
}
