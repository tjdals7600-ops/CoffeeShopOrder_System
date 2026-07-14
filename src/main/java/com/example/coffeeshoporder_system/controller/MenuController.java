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

    @GetMapping
    public MenusResponse getMenus() {
        return menuService.getMenus();
    }

    @GetMapping("/popular")
    public PopularMenusResponse getPopularMenus(
            @RequestParam(defaultValue = "WEEKLY") String period,
            @RequestParam(defaultValue = "3") int limit
    ) {
        return menuService.getPopularMenus(period, limit);
    }
}
