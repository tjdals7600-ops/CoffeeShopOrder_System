package com.example.coffeeshoporder_system.controller;

import com.example.coffeeshoporder_system.dto.response.MenusResponse;
import com.example.coffeeshoporder_system.dto.response.PopularMenusResponse;
import com.example.coffeeshoporder_system.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService menuService;

    // 판매 중인 커피 메뉴 목록을 조회합니다.
    @GetMapping
    public MenusResponse getMenus() {
        return menuService.getMenus();
    }

    // 최근 7일 기준 인기 메뉴 TOP 3을 기본값으로 조회합니다.
    @GetMapping("/popular")
    public PopularMenusResponse getPopularMenus(
            @RequestParam(defaultValue = "WEEKLY") String period,
            @RequestParam(defaultValue = "3") int limit
    ) {
        // period와 limit을 query parameter로 받아 확장 가능하게 둡니다.
        return menuService.getPopularMenus(period, limit);
    }
}
