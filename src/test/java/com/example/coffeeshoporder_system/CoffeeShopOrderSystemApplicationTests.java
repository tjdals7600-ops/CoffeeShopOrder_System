package com.example.coffeeshoporder_system;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class CoffeeShopOrderSystemApplicationTests {

	// Spring context가 test profile로 정상 기동되는지 확인합니다.
	@Test
	void contextLoads() {
	}

}
