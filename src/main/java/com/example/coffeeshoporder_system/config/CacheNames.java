package com.example.coffeeshoporder_system.config;

public final class CacheNames {

    // 판매 메뉴 목록은 화면 표시용 캐시이며, 결제 금액 계산에는 사용하지 않습니다.
    public static final String SELLING_MENUS = "sellingMenus";

    // 인기 메뉴는 커밋된 주문 집계 데이터를 기반으로 만든 조회용 캐시입니다.
    public static final String POPULAR_MENUS = "popularMenus";

    private CacheNames() {
    }
}
