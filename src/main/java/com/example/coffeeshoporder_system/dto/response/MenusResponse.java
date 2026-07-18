package com.example.coffeeshoporder_system.dto.response;

import java.util.List;

public record MenusResponse(
        // 판매 중인 메뉴 목록입니다.
        List<MenuResponse> menus
) {
}
