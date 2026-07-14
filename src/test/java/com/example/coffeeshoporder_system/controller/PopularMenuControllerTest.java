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

    @BeforeEach
    void setUp() {
        orderOutboxRepository.deleteAll();
        pointHistoryRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        userPointRepository.deleteAll();
        menuOrderStatRepository.deleteAll();
        menuRepository.deleteAll();
    }

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

    @Test
    void getPopularMenusFailsWhenPeriodIsInvalid() throws Exception {
        mockMvc.perform(get("/api/menus/popular")
                        .param("period", "YEARLY")
                        .param("limit", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    private Menu saveMenu(String name, Long price) {
        return menuRepository.save(Menu.builder()
                .name(name)
                .price(price)
                .description(name + " 설명")
                .status(MenuStatus.SELLING)
                .build());
    }

    private void saveStat(Menu menu, Long orderCount, Long totalQuantity, Long totalSalesPoint) {
        saveStat(menu, LocalDate.now(), orderCount, totalQuantity, totalSalesPoint);
    }

    private void saveStat(Menu menu, LocalDate statDate, Long orderCount, Long totalQuantity, Long totalSalesPoint) {
        menuOrderStatRepository.save(MenuOrderStat.builder()
                .menu(menu)
                .statDate(statDate)
                .orderCount(orderCount)
                .totalQuantity(totalQuantity)
                .totalSalesPoint(totalSalesPoint)
                .build());
    }
}
