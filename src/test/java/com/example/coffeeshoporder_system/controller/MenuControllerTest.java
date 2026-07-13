package com.example.coffeeshoporder_system.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.coffeeshoporder_system.domain.entity.Menu;
import com.example.coffeeshoporder_system.domain.type.MenuStatus;
import com.example.coffeeshoporder_system.repository.MenuRepository;
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
class MenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MenuRepository menuRepository;

    @BeforeEach
    void setUp() {
        menuRepository.deleteAll();
        menuRepository.save(Menu.builder()
                .name("아메리카노")
                .price(4000L)
                .description("진한 에스프레소 기반 커피")
                .status(MenuStatus.SELLING)
                .build());
        menuRepository.save(Menu.builder()
                .name("카페라떼")
                .price(4500L)
                .description("우유가 들어간 부드러운 커피")
                .status(MenuStatus.SELLING)
                .build());
        menuRepository.save(Menu.builder()
                .name("시즌 종료 메뉴")
                .price(5000L)
                .description("판매 중지 메뉴")
                .status(MenuStatus.STOPPED)
                .build());
    }

    @Test
    void getMenusReturnsSellingMenuSummaries() throws Exception {
        mockMvc.perform(get("/api/menus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menus.length()").value(2))
                .andExpect(jsonPath("$.menus[0].menuId").exists())
                .andExpect(jsonPath("$.menus[0].name").value("아메리카노"))
                .andExpect(jsonPath("$.menus[0].price").value(4000))
                .andExpect(jsonPath("$.menus[0].description").doesNotExist())
                .andExpect(jsonPath("$.menus[1].name").value("카페라떼"))
                .andExpect(jsonPath("$.menus[1].price").value(4500));
    }
}
