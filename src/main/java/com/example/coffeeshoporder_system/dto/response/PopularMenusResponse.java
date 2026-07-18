package com.example.coffeeshoporder_system.dto.response;

import java.util.List;

public record PopularMenusResponse(
        // 인기 메뉴 순위를 계산한 기간입니다.
        String period,

        // 집계 테이블에서 조회한 인기 메뉴 순위 목록입니다.
        List<PopularMenuResponse> menus
) {
}
