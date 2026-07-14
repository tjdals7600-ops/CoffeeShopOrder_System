package com.example.coffeeshoporder_system.dto.response;

import java.util.List;

public record PopularMenusResponse(
        String period,
        List<PopularMenuResponse> menus
) {
}
