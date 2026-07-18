package com.example.coffeeshoporder_system.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.coffeeshoporder_system.domain.entity.Menu;
import com.example.coffeeshoporder_system.domain.entity.MenuOrderStat;
import com.example.coffeeshoporder_system.domain.type.MenuStatus;
import com.example.coffeeshoporder_system.repository.MenuOrderStatRepository;
import com.example.coffeeshoporder_system.repository.MenuRepository;
import com.example.coffeeshoporder_system.repository.OrderItemRepository;
import com.example.coffeeshoporder_system.repository.OrderOutboxRepository;
import com.example.coffeeshoporder_system.repository.OrderRepository;
import com.example.coffeeshoporder_system.repository.PointHistoryRepository;
import com.example.coffeeshoporder_system.repository.UserPointRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class PopularMenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private MenuOrderStatRepository menuOrderStatRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderOutboxRepository orderOutboxRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @Autowired
    private UserPointRepository userPointRepository;

    @Autowired
    private CacheManager cacheManager;

    // 인기 메뉴 집계 테스트가 독립적으로 실행되도록 관련 테이블과 캐시를 초기화합니다.
    @BeforeEach
    void setUp() {
        orderOutboxRepository.deleteAll();
        pointHistoryRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        userPointRepository.deleteAll();
        menuOrderStatRepository.deleteAll();
        menuRepository.deleteAll();
        clearCaches();
    }

    // 지정 기간과 limit으로 인기 메뉴 목록이 주문 횟수 순서대로 반환되는지 검증합니다.
    @Test
    void getPopularMenus() throws Exception {
        Menu americano = saveMenu("아메리카노", 4000L);
        Menu latte = saveMenu("카페라떼", 4500L);
        saveStat(americano, 120L, 180L, 720000L);
        saveStat(latte, 95L, 130L, 585000L);

        mockMvc.perform(get("/api/menus/popular")
                        .param("period", "DAILY")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("DAILY"))
                .andExpect(jsonPath("$.menus.length()").value(2))
                .andExpect(jsonPath("$.menus[0].menuId").value(americano.getId()))
                .andExpect(jsonPath("$.menus[0].name").value("아메리카노"))
                .andExpect(jsonPath("$.menus[0].price").value(4000))
                .andExpect(jsonPath("$.menus[0].orderCount").value(120))
                .andExpect(jsonPath("$.menus[0].totalQuantity").value(180))
                .andExpect(jsonPath("$.menus[1].menuId").value(latte.getId()));
    }

    // 기본 인기 메뉴 조회가 최근 7일 기준 TOP 3와 정확한 주문 횟수를 반환하는지 검증합니다.
    @Test
    void getPopularMenusReturnsTop3ByOrderCountForRecent7Days() throws Exception {
        Menu first = saveMenu("아메리카노", 4000L);
        Menu second = saveMenu("카페라떼", 4500L);
        Menu third = saveMenu("바닐라라떼", 5500L);
        Menu fourth = saveMenu("모카라떼", 5500L);

        saveStat(first, LocalDate.now(), 5L, 30L, 120000L);
        saveStat(first, LocalDate.now().minusDays(6), 10L, 10L, 40000L);
        saveStat(first, LocalDate.now().minusDays(7), 100L, 100L, 400000L);
        saveStat(second, LocalDate.now().minusDays(1), 12L, 50L, 225000L);
        saveStat(third, LocalDate.now().minusDays(2), 9L, 9L, 49500L);
        saveStat(fourth, LocalDate.now().minusDays(3), 8L, 100L, 550000L);

        mockMvc.perform(get("/api/menus/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("WEEKLY"))
                .andExpect(jsonPath("$.menus.length()").value(3))
                .andExpect(jsonPath("$.menus[0].menuId").value(first.getId()))
                .andExpect(jsonPath("$.menus[0].orderCount").value(15))
                .andExpect(jsonPath("$.menus[0].totalQuantity").value(40))
                .andExpect(jsonPath("$.menus[1].menuId").value(second.getId()))
                .andExpect(jsonPath("$.menus[1].orderCount").value(12))
                .andExpect(jsonPath("$.menus[2].menuId").value(third.getId()))
                .andExpect(jsonPath("$.menus[2].orderCount").value(9));
    }

    // 반복 조회 시 집계 테이블이 바뀌어도 캐시된 결과가 반환되는지 검증합니다.
    @Test
    void getPopularMenusUsesCacheForRepeatedReads() throws Exception {
        Menu americano = saveMenu("cached-americano", 4000L);
        Menu latte = saveMenu("cached-latte", 4500L);
        saveStat(americano, 10L, 10L, 40000L);

        mockMvc.perform(get("/api/menus/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menus.length()").value(1))
                .andExpect(jsonPath("$.menus[0].menuId").value(americano.getId()));

        saveStat(latte, 99L, 99L, 445500L);

        mockMvc.perform(get("/api/menus/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menus.length()").value(1))
                .andExpect(jsonPath("$.menus[0].menuId").value(americano.getId()));
    }

    // 지원하지 않는 기간 값은 INVALID_REQUEST로 거절되는지 검증합니다.
    @Test
    void getPopularMenusFailsWhenPeriodIsInvalid() throws Exception {
        mockMvc.perform(get("/api/menus/popular")
                        .param("period", "YEARLY")
                        .param("limit", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    // 인기 메뉴 테스트에 필요한 판매 중 메뉴 fixture를 저장합니다.
    private Menu saveMenu(String name, Long price) {
        return menuRepository.save(Menu.builder()
                .name(name)
                .price(price)
                .description(name + " 설명")
                .status(MenuStatus.SELLING)
                .build());
    }

    // 오늘 날짜 기준 집계 fixture를 저장합니다.
    private void saveStat(Menu menu, Long orderCount, Long totalQuantity, Long totalSalesPoint) {
        saveStat(menu, LocalDate.now(), orderCount, totalQuantity, totalSalesPoint);
    }

    // 원하는 날짜의 메뉴별 집계 fixture를 저장합니다.
    private void saveStat(Menu menu, LocalDate statDate, Long orderCount, Long totalQuantity, Long totalSalesPoint) {
        menuOrderStatRepository.save(MenuOrderStat.builder()
                .menu(menu)
                .statDate(statDate)
                .orderCount(orderCount)
                .totalQuantity(totalQuantity)
                .totalSalesPoint(totalSalesPoint)
                .build());
    }

    // 테스트용 simple cache에 남은 인기 메뉴 결과를 비웁니다.
    private void clearCaches() {
        cacheManager.getCacheNames()
                .forEach(name -> {
                    var cache = cacheManager.getCache(name);
                    if (cache != null) {
                        cache.clear();
                    }
                });
    }
}
