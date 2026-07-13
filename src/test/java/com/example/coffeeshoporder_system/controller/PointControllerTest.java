package com.example.coffeeshoporder_system.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.coffeeshoporder_system.repository.PointHistoryRepository;
import com.example.coffeeshoporder_system.repository.UserPointRepository;
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
class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserPointRepository userPointRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @BeforeEach
    void setUp() {
        pointHistoryRepository.deleteAll();
        userPointRepository.deleteAll();
    }

    @Test
    void chargePoints() throws Exception {
        mockMvc.perform(post("/api/points/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "requestId": "20260713-user1-charge-001",
                                  "amount": 10000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.chargedAmount").value(10000))
                .andExpect(jsonPath("$.balance").value(10000));

        var userPoint = userPointRepository.findByUserId(1L).orElseThrow();
        assertThat(userPoint.getBalance()).isEqualTo(10000L);
        assertThat(pointHistoryRepository.count()).isEqualTo(1);
    }

    @Test
    void chargePointsWithSameRequestIdReturnsPreviousResult() throws Exception {
        String requestBody = """
                {
                  "userId": 1,
                  "requestId": "20260713-user1-charge-duplicate",
                  "amount": 10000
                }
                """;

        mockMvc.perform(post("/api/points/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(10000));

        mockMvc.perform(post("/api/points/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.chargedAmount").value(10000))
                .andExpect(jsonPath("$.balance").value(10000));

        var userPoint = userPointRepository.findByUserId(1L).orElseThrow();
        assertThat(userPoint.getBalance()).isEqualTo(10000L);
        assertThat(pointHistoryRepository.count()).isEqualTo(1);
    }

    @Test
    void chargePointsFailsWhenAmountIsNotPositive() throws Exception {
        mockMvc.perform(post("/api/points/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "requestId": "20260713-user1-charge-invalid",
                                  "amount": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_POINT_AMOUNT"));
    }
}
