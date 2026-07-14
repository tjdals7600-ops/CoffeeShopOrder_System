package com.example.coffeeshoporder_system.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.coffeeshoporder_system.domain.entity.Menu;
import com.example.coffeeshoporder_system.domain.entity.UserPoint;
import com.example.coffeeshoporder_system.domain.type.MenuStatus;
import com.example.coffeeshoporder_system.repository.MenuOrderStatRepository;
import com.example.coffeeshoporder_system.repository.MenuRepository;
import com.example.coffeeshoporder_system.repository.OrderItemRepository;
import com.example.coffeeshoporder_system.repository.OrderOutboxRepository;
import com.example.coffeeshoporder_system.repository.OrderRepository;
import com.example.coffeeshoporder_system.repository.PointHistoryRepository;
import com.example.coffeeshoporder_system.repository.UserPointRepository;
import com.example.coffeeshoporder_system.service.InMemoryDataCollectionPlatformClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private MenuOrderStatRepository menuOrderStatRepository;

    @Autowired
    private UserPointRepository userPointRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderOutboxRepository orderOutboxRepository;

    @Autowired
    private InMemoryDataCollectionPlatformClient dataCollectionPlatformClient;

    @BeforeEach
    void setUp() {
        orderOutboxRepository.deleteAll();
        pointHistoryRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        userPointRepository.deleteAll();
        menuOrderStatRepository.deleteAll();
        menuRepository.deleteAll();
        dataCollectionPlatformClient.clear();
    }

    @Test
    void orderAndPayWithPoint() throws Exception {
        Menu menu = saveMenu("아메리카노", 4000L, MenuStatus.SELLING);
        saveUserPoint(1L, 10000L);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "requestId": "20260714-user1-order-001",
                                  "menuId": %d
                                }
                                """.formatted(menu.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.totalPrice").value(4000))
                .andExpect(jsonPath("$.balance").value(6000))
                .andExpect(jsonPath("$.items[0].menuId").value(menu.getId()))
                .andExpect(jsonPath("$.items[0].menuName").value("아메리카노"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(4000))
                .andExpect(jsonPath("$.items[0].quantity").value(1))
                .andExpect(jsonPath("$.items[0].linePrice").value(4000));

        assertThat(userPointRepository.findByUserId(1L).orElseThrow().getBalance()).isEqualTo(6000L);
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(orderItemRepository.count()).isEqualTo(1);
        assertThat(pointHistoryRepository.count()).isEqualTo(1);
        assertThat(orderOutboxRepository.count()).isEqualTo(1);
        assertThat(dataCollectionPlatformClient.getSentEvents()).hasSize(1);
        assertThat(dataCollectionPlatformClient.getSentEvents().getFirst().totalPrice()).isEqualTo(4000L);
    }

    @Test
    void orderAndPayMultipleItemsWithPoint() throws Exception {
        Menu americano = saveMenu("아메리카노", 4000L, MenuStatus.SELLING);
        Menu latte = saveMenu("카페 \"라떼\"", 4500L, MenuStatus.SELLING);
        saveUserPoint(1L, 20000L);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "requestId": "20260714-user1-order-multiple",
                                  "items": [
                                    {
                                      "menuId": %d,
                                      "quantity": 2
                                    },
                                    {
                                      "menuId": %d,
                                      "quantity": 1
                                    }
                                  ]
                                }
                                """.formatted(americano.getId(), latte.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.totalPrice").value(12500))
                .andExpect(jsonPath("$.balance").value(7500))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].linePrice").value(8000))
                .andExpect(jsonPath("$.items[1].menuName").value("카페 \"라떼\""))
                .andExpect(jsonPath("$.items[1].quantity").value(1))
                .andExpect(jsonPath("$.items[1].linePrice").value(4500));

        assertThat(userPointRepository.findByUserId(1L).orElseThrow().getBalance()).isEqualTo(7500L);
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(orderItemRepository.count()).isEqualTo(2);
        assertThat(orderOutboxRepository.findAll().getFirst().getPayload()).contains("\\\"라떼\\\"");
        assertThat(dataCollectionPlatformClient.getSentEvents()).hasSize(1);
        assertThat(dataCollectionPlatformClient.getSentEvents().getFirst().items()).hasSize(2);

        mockMvc.perform(get("/api/menus/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menus.length()").value(2))
                .andExpect(jsonPath("$.menus[0].menuId").value(americano.getId()))
                .andExpect(jsonPath("$.menus[0].orderCount").value(1))
                .andExpect(jsonPath("$.menus[0].totalQuantity").value(2))
                .andExpect(jsonPath("$.menus[1].menuId").value(latte.getId()))
                .andExpect(jsonPath("$.menus[1].orderCount").value(1))
                .andExpect(jsonPath("$.menus[1].totalQuantity").value(1));
    }

    @Test
    void sameRequestIdDoesNotPayTwice() throws Exception {
        Menu menu = saveMenu("카페라떼", 4500L, MenuStatus.SELLING);
        saveUserPoint(1L, 10000L);
        String requestBody = """
                {
                  "userId": 1,
                  "requestId": "20260714-user1-order-duplicate",
                  "menuId": %d
                }
                """.formatted(menu.getId());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(5500));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.totalPrice").value(4500))
                .andExpect(jsonPath("$.balance").value(5500));

        assertThat(userPointRepository.findByUserId(1L).orElseThrow().getBalance()).isEqualTo(5500L);
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(orderItemRepository.count()).isEqualTo(1);
        assertThat(pointHistoryRepository.count()).isEqualTo(1);
        assertThat(orderOutboxRepository.count()).isEqualTo(1);
        assertThat(dataCollectionPlatformClient.getSentEvents()).hasSize(1);
    }

    @Test
    void orderFailsWhenPointIsInsufficient() throws Exception {
        Menu menu = saveMenu("바닐라라떼", 5500L, MenuStatus.SELLING);
        saveUserPoint(1L, 1000L);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "requestId": "20260714-user1-order-insufficient",
                                  "menuId": %d
                                }
                                """.formatted(menu.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_POINT"));

        assertThat(userPointRepository.findByUserId(1L).orElseThrow().getBalance()).isEqualTo(1000L);
        assertThat(orderRepository.count()).isZero();
        assertThat(orderOutboxRepository.count()).isZero();
        assertThat(dataCollectionPlatformClient.getSentEvents()).isEmpty();
    }

    @Test
    void orderFailsWhenMenuIsNotSelling() throws Exception {
        Menu menu = saveMenu("판매 중지 메뉴", 5000L, MenuStatus.STOPPED);
        saveUserPoint(1L, 10000L);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "requestId": "20260714-user1-order-stopped",
                                  "menuId": %d
                                }
                                """.formatted(menu.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MENU_NOT_SELLING"));
    }

    @Test
    void orderFailsWhenQuantityIsNotPositive() throws Exception {
        Menu menu = saveMenu("아메리카노", 4000L, MenuStatus.SELLING);
        saveUserPoint(1L, 10000L);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "requestId": "20260714-user1-order-invalid-quantity",
                                  "items": [
                                    {
                                      "menuId": %d,
                                      "quantity": 0
                                    }
                                  ]
                                }
                                """.formatted(menu.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ORDER_QUANTITY"));

        assertThat(orderRepository.count()).isZero();
        assertThat(orderOutboxRepository.count()).isZero();
    }

    private Menu saveMenu(String name, Long price, MenuStatus status) {
        return menuRepository.save(Menu.builder()
                .name(name)
                .price(price)
                .description(name + " 설명")
                .status(status)
                .build());
    }

    private void saveUserPoint(Long userId, Long balance) {
        userPointRepository.save(UserPoint.builder()
                .userId(userId)
                .balance(balance)
                .build());
    }
}
