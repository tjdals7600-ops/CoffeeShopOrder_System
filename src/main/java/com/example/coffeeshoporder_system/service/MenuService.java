package com.example.coffeeshoporder_system.service;

import com.example.coffeeshoporder_system.config.CacheNames;
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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final MenuOrderStatRepository menuOrderStatRepository;

    // 판매 중인 메뉴 목록은 projection으로 필요한 컬럼만 조회하고 Redis 캐시에 저장합니다.
    @Cacheable(cacheNames = CacheNames.SELLING_MENUS, key = "'all'")
    @Transactional(readOnly = true)
    public MenusResponse getMenus() {
        // 캐시는 조회 응답에만 사용하고, 주문/결제 금액은 항상 DB에서 다시 확인합니다.
        var menus = menuRepository.findMenuSummariesByStatusOrderByIdAsc(MenuStatus.SELLING)
                .stream()
                .map(menu -> new MenuResponse(menu.getMenuId(), menu.getName(), menu.getPrice()))
                .toList();

        return new MenusResponse(menus);
    }

    // 인기 메뉴는 메뉴별 일자 집계 테이블에서 조회해 주문 원장 스캔을 피합니다.
    @Cacheable(cacheNames = CacheNames.POPULAR_MENUS, key = "#periodValue.toUpperCase() + ':' + #limit")
    @Transactional(readOnly = true)
    public PopularMenusResponse getPopularMenus(String periodValue, int limit) {
        // Redis 캐시 키는 기간과 limit 조합으로 분리됩니다.
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

    // API query parameter의 기간 값을 enum으로 변환합니다.
    private PopularMenuPeriod parsePeriod(String periodValue) {
        try {
            return PopularMenuPeriod.valueOf(periodValue.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "period는 DAILY, WEEKLY, MONTHLY 중 하나여야 합니다.");
        }
    }

    // 과도한 조회 크기를 막기 위해 limit 상한을 둡니다.
    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "limit은 0보다 커야 합니다.");
        }
        return Math.min(limit, 100);
    }

    // 기간별 조회 시작일을 계산합니다. WEEKLY는 오늘을 포함한 최근 7일입니다.
    private LocalDate getStartDate(PopularMenuPeriod period, LocalDate endDate) {
        return switch (period) {
            case DAILY -> endDate;
            case WEEKLY -> endDate.minusDays(6);
            case MONTHLY -> endDate.withDayOfMonth(1);
        };
    }
}
