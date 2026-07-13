package com.example.coffeeshoporder_system.controller;

import com.example.coffeeshoporder_system.dto.request.PointChargeRequest;
import com.example.coffeeshoporder_system.dto.response.PointChargeResponse;
import com.example.coffeeshoporder_system.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/points")
public class PointController {

    private final PointService pointService;

    @PostMapping("/charge")
    public PointChargeResponse charge(@RequestBody PointChargeRequest request) {
        return pointService.charge(request);
    }
}
