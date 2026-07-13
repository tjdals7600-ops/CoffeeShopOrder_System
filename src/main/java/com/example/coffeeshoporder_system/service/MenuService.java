package com.example.coffeeshoporder_system.service;

import com.example.coffeeshoporder_system.domain.type.MenuStatus;
import com.example.coffeeshoporder_system.dto.response.MenuResponse;
import com.example.coffeeshoporder_system.dto.response.MenusResponse;
import com.example.coffeeshoporder_system.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;

    @Transactional(readOnly = true)
    public MenusResponse getMenus() {
        var menus = menuRepository.findMenuSummariesByStatusOrderByIdAsc(MenuStatus.SELLING)
                .stream()
                .map(menu -> new MenuResponse(menu.getMenuId(), menu.getName(), menu.getPrice()))
                .toList();

        return new MenusResponse(menus);
    }
}
