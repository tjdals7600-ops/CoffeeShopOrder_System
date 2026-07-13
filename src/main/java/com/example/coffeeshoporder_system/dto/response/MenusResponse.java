package com.example.coffeeshoporder_system.dto.response;

import java.util.List;

public record MenusResponse(
        List<MenuResponse> menus
) {
}
