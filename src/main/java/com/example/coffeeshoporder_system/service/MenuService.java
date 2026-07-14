package com.example.coffeeshoporder_system.service;

import com.example.coffeeshoporder_system.domain.type.MenuStatus;
import com.example.coffeeshoporder_system.domain.type.PopularMenuPeriod;
import com.example.coffeeshoporder_system.dto.response.MenuResponse;
import com.example.coffeeshoporder_system.dto.response.MenusResponse;
import com.example.coffeeshoporder_system.dto.response.PopularMenuResponse;
import com.example.coffeeshoporder_system.dto.response.PopularMenusResponse;
import com.example.coffeeshoporder_system.exception.CustomException;
import com.example.coffeeshoporder_system.exception.ErrorCode;
import com.example.coffeeshoporder_system.repository.MenuOrderStatRepository;
import com.example.coffeeshoporder_system.repository.MenuRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final MenuOrderStatRepository menuOrderStatRepository;

    @Transactional(readOnly = true)
    public MenusResponse getMenus() {
        var menus = menuRepository.findMenuSummariesByStatusOrderByIdAsc(MenuStatus.SELLING)
                .stream()
                .map(menu -> new MenuResponse(menu.getMenuId(), menu.getName(), menu.getPrice()))
                .toList();

        return new MenusResponse(menus);
    }

    @Transactional(readOnly = true)
    public PopularMenusResponse getPopularMenus(String periodValue, int limit) {
        PopularMenuPeriod period = parsePeriod(periodValue);
        int normalizedLimit = normalizeLimit(limit);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = getStartDate(period, endDate);

        var menus = menuOrderStatRepository.findPopularMenus(
                        startDate,
                        endDate,
                        PageRequest.of(0, normalizedLimit)
                )
                .stream()
                .map(menu -> new PopularMenuResponse(
                        menu.getMenuId(),
                        menu.getName(),
                        menu.getPrice(),
                        menu.getOrderCount(),
                        menu.getTotalQuantity()
                ))
                .toList();

        return new PopularMenusResponse(period.name(), menus);
    }

    private PopularMenuPeriod parsePeriod(String periodValue) {
        try {
            return PopularMenuPeriod.valueOf(periodValue.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "period는 DAILY, WEEKLY, MONTHLY 중 하나여야 합니다.");
        }
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "limit은 0보다 커야 합니다.");
        }
        return Math.min(limit, 100);
    }

    private LocalDate getStartDate(PopularMenuPeriod period, LocalDate endDate) {
        return switch (period) {
            case DAILY -> endDate;
            case WEEKLY -> endDate.minusDays(6);
            case MONTHLY -> endDate.withDayOfMonth(1);
        };
    }
}
