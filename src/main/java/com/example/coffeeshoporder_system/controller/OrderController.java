package com.example.coffeeshoporder_system.controller;

import com.example.coffeeshoporder_system.dto.request.OrderRequest;
import com.example.coffeeshoporder_system.dto.response.OrderResponse;
import com.example.coffeeshoporder_system.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public OrderResponse order(@RequestBody OrderRequest request) {
        return orderService.order(request);
    }
}
